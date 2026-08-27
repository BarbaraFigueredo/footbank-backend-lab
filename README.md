# FootBank - Laboratório de Engenharia e System Design

Bem-vindo ao **FootBank**, um laboratório prático de engenharia de software focado em simular cenários de alta complexidade, resiliência e integridade financeira para uma fintech do mercado esportivo.

O objetivo deste repositório é documentar a resolução de incidentes críticos de produção em sistemas desenvolvidos com **Java 17+**, **Spring Boot** e conceitos avançados de **System Design**.

---

## Arquitetura e Escopo do Projeto

A aplicação simula um ecossistema de microsserviços financeiro voltado para clubes de futebol, processamento de ingressos (ticketing) e gerenciamento de carteiras digitais (wallets).

Cada pasta dentro de `src/` representa um incidente real de Big Tech (Severidade 1 ou 2) que foi investigado, mitigado e corrigido através de revisões de código e padrões de arquitetura corporativa.

---

## Tecnologias e Conceitos de Engenharia Aplicados

Ao longo das simulações, foram implementados e consolidados os seguintes tópicos de arquitetura:

- **Idempotência em APIs Financeiras:** Prevenção de duplicidade de transações causadas por reenvios de requisições.
- **Padrões de Resiliência (Tolerância a Falhas):** Implementação conceitual de *Circuit Breakers* e estratégias de *Rollback* em falhas de integrações de APIs de terceiros.
- **Performance no Java Moderno:** Uso eficiente de *Streams API* para manipulação de grandes volumes de dados em memória.
- **Programação Orientada a Objetos Avançada:** Utilização de herança, encapsulamento e polimorfismo para modularização de regras de negócio complexas.

---

## Registro de Incidentes Resolvidos

Acompanhe a evolução do laboratório e a análise detalhada de cada caso técnico nas subpastas:

1. **[Caso 01 - Idempotência](./src/01-idempotencia):** Tratamento de requisições duplicadas em cliques repetidos no fluxo de pagamento.
2. *(Adicione os próximos casos aqui conforme for evoluindo!)*

---

## Autora

- **Bárbara de Figueredo Matias**
- [LinkedIn](https://www.linkedin.com/in/barbarafigueredo)