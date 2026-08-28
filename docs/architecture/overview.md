# FootBank — Visão Geral da Arquitetura

## Sistema

FootBank é uma fintech voltada ao mercado de futebol. Permite que torcedores comprem ingressos, gerenciem carteiras digitais e realizem pagamentos.

## Domínios

```
src/main/java/com/footbank/

├── payment/     — processamento de pagamentos e integração com gateways
├── wallet/      — carteira digital dos usuários (a implementar)
├── ticket/      — compra e reserva de ingressos (a implementar)
├── audit/       — auditoria de operações (a implementar)
├── config/      — configurações da aplicação
└── shared/      — código compartilhado entre domínios
```

## Fluxo atual

```
Cliente
  ↓
POST /api/payments
  ↓
PaymentController
  ↓
PaymentGatewayService
  ↓
[Circuit Breaker]
  ↓ CLOSED              ↓ OPEN
PagBrasilPay API    Fallback → PENDING
```

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 17 | Linguagem principal |
| Spring Boot 3.3 | Framework web e DI |
| Spring Data JPA | Persistência |
| Resilience4j | Circuit Breaker (adicionado no incidente 002) |
| Maven | Build |

## Decisões arquiteturais

- [ADR-001-circuit-breaker.md](decisions/ADR-001-circuit-breaker.md) — Adoção do Resilience4j para proteção de integrações externas
