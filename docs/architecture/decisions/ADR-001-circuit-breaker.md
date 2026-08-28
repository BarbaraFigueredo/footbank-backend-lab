# ADR-001 — Circuit Breaker para integrações externas

## Contexto

O FootBank integra com gateways de pagamento externos. Essas integrações são pontos de falha que podem derrubar todo o sistema caso o serviço externo fique indisponível ou lento.

## Problema

Sem proteção, uma chamada bloqueante a um gateway lento esgota o thread pool do serviço, causando cascata de falhas nos serviços dependentes.

## Alternativas consideradas

| Alternativa | Avaliação |
|---|---|
| Timeout simples | Limita o tempo de espera, mas não evita sobrecarga com múltiplas tentativas |
| Retry com backoff | Útil para falhas transitórias, mas piora a situação em falhas prolongadas |
| Circuit Breaker | Para de chamar o serviço com problema e retorna fallback imediatamente |
| Bulkhead | Isola threads por serviço, mas não evita o bloqueio individual |

## Decisão

Adotar **Resilience4j Circuit Breaker** como mecanismo padrão de proteção para todas as integrações com serviços externos.

## Justificativa

- Biblioteca nativa do ecossistema Spring Boot 3
- Suporte a anotações (`@CircuitBreaker`) com baixo acoplamento
- Configuração via `application.yml`
- Integração com Spring Actuator para observabilidade dos estados do circuito

## Trade-offs

- Adiciona complexidade de configuração (thresholds, timeouts, fallbacks)
- O fallback deve ser implementado para cada método protegido
- Pagamentos em PENDING precisam de mecanismo de reprocessamento futuro

## Consequências

- Toda integração com serviço externo deve usar `@CircuitBreaker`
- Fallbacks devem ser definidos e testados
- Futuramente: implementar job de reprocessamento de pagamentos PENDING
