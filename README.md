# FootBank — Backend Lab

FootBank é uma fintech fictícia voltada ao mercado de futebol. Este repositório é um laboratório prático de engenharia de software backend, onde o sistema evolui continuamente através de **features**, **incidentes** e **desafios de escala**.

O código não é uma coleção de exercícios isolados — é um sistema real que cresce a cada desafio.

---

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 17 |
| Spring Boot | 3.3 |
| Spring Data JPA | — |
| Resilience4j | 2.2.0 |
| Maven | — |

---

## Estrutura do projeto

```
footbank/
│
├── src/
│   └── main/
│       ├── java/com/footbank/
│       │   ├── FootbankApplication.java
│       │   ├── config/           — configurações da aplicação
│       │   ├── payment/          — domínio de pagamentos
│       │   ├── wallet/           — carteira digital (a implementar)
│       │   ├── ticket/           — ingressos (a implementar)
│       │   └── audit/            — auditoria (a implementar)
│       └── resources/
│           └── application.yml
│
├── docs/
│   ├── architecture/             — visão geral e decisões arquiteturais (ADRs)
│   ├── incidents/                — registro de incidentes resolvidos
│   ├── features/                 — documentação de funcionalidades
│   └── scale/                    — desafios de escalabilidade
│
├── pom.xml
└── README.md
```

---

## Incidentes resolvidos

| # | Título | Conceito | Severidade |
|---|---|---|---|
| [001](docs/incidents/001-payment-duplication.md) | Duplicidade de Pagamentos | Idempotência | SEV-1 |
| [002](docs/incidents/002-circuit-breaker-cascade-failure.md) | Cascata de Falhas no payment-service | Circuit Breaker | SEV-1 |

---

## Documentação

- [Visão geral da arquitetura](docs/architecture/overview.md)
- [ADR-001 — Circuit Breaker para integrações externas](docs/architecture/decisions/ADR-001-circuit-breaker.md)

---

## Autora

**Bárbara de Figueredo Matias**
[LinkedIn](https://www.linkedin.com/in/barbarafigueredo)
