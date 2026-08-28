# 002 — Cascata de Falhas no payment-service

## Contexto

O `payment-service` integra com o gateway externo PagBrasilPay para processar pagamentos de ingressos e carteiras digitais.

## Problema

O gateway PagBrasilPay entrou em alta latência (30s+ por requisição). Como o serviço chamava o gateway diretamente sem proteção, todas as threads do `payment-service` ficaram bloqueadas aguardando resposta. A lentidão se propagou em cascata para o `wallet-service` e o `ticketing-service`.

## Impacto

- Taxa de erro: 78%
- P99 latência: 25s+ (SLO violado: limite era 500ms)
- Serviços afetados: `payment-service`, `wallet-service`, `ticketing-service`

## Causa raiz

Chamada direta ao gateway externo sem nenhum mecanismo de proteção. Quando o PagBrasilPay ficou lento, as threads do serviço ficaram bloqueadas indefinidamente, esgotando o thread pool e impedindo que novas requisições fossem atendidas.

## Investigação

```
payment-service → PagBrasilPay (30s timeout)
                ↑
           threads bloqueadas
                ↓
wallet-service → payment-service (sem resposta)
ticketing-service → payment-service (sem resposta)
```

## Conceito estudado

**Circuit Breaker** — padrão de resiliência que monitora falhas em chamadas externas e, ao atingir um limiar, "abre o circuito" bloqueando novas chamadas e retornando um fallback imediatamente. Possui três estados: CLOSED, OPEN e HALF-OPEN.

## Solução

Adicionado Circuit Breaker via Resilience4j no método `processPayment` com fallback que:
1. Persiste o pagamento com status `PENDING`
2. Notifica o cliente por email
3. Retorna `PaymentConfirmation` com status `PENDING`

O controller passou a retornar HTTP `202 Accepted` quando o pagamento está pendente.

## Arquitetura antes

```
PaymentController
      ↓
PaymentGatewayService
      ↓  (chamada direta, sem proteção)
PagBrasilPay API
```

## Arquitetura depois

```
PaymentController
      ↓
PaymentGatewayService
      ↓
[Circuit Breaker - Resilience4j]
      ↓ CLOSED          ↓ OPEN
PagBrasilPay API   processPaymentFallback()
                         ↓
                   PaymentRepository (PENDING)
                   NotificationService (email)
```

## Trade-offs

| Decisão | Vantagem | Desvantagem |
|---|---|---|
| Circuit Breaker com fallback | Sistema continua operando | Pagamento não é processado imediatamente |
| Status PENDING | Pagamento não se perde | Precisa de reprocessamento posterior |
| waitDurationInOpenState: 30s | Dá tempo ao gateway se recuperar | Pode ser longo demais em alguns cenários |

## Configuração aplicada

```yaml
resilience4j:
  circuitbreaker:
    instances:
      pagBrasilPay:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
```

## Lições aprendidas

- Toda integração com serviço externo precisa de proteção (Circuit Breaker, timeout, retry)
- Cascata de falhas acontece quando um serviço lento bloqueia threads que outros serviços precisam
- Em sistemas financeiros, é melhor retornar `PENDING` do que retornar erro — o pagamento não se perde
- O limiar baseado em percentual (50% de falhas) é mais estável do que contagem fixa de erros
