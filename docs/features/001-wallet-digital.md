# Integração do Módulo de Carteira Digital (Wallet) e Orquestração Resiliente de Pagamentos

## Contexto
A FootBank opera como a fintech oficial de processamento de compras de ingressos e movimentações financeiras para clubes de futebol. Na Fase 1 do projeto, a arquitetura do sistema é baseada em um monólito desenvolvido com Java 17+, Spring Boot, Spring Data JPA e banco de dados em memória H2 (configurado em escopo de runtime). O ecossistema é composto pelos domínios de `Wallet` e `Payment`.

## Problema
Durante cenários de alta concorrência e instabilidade de rede (como a abertura de bilheterias de grandes clássicos), torcedores clicavam múltiplas vezes no botão de compra do aplicativo. Sem uma validação prévia de saldo e sem um fluxo estruturado, requisições concorrentes eram enviadas diretamente ao gateway de pagamento externo (`pagBrasilPay`). Caso o gateway respondesse com sucesso ou sofresse oscilações, o sistema gerava inconsistências operacionais e falhas de sincronia entre o saldo disponível da carteira e a confirmação da transação.

## Impacto
O impacto direto era a insatisfação dos torcedores e prejuízos operacionais. O saldo dos clientes era descontado indevidamente em tentativas duplicadas ou o fluxo falhava por completo ao tentar conectar-se com o gateway instável, gerando falhas críticas de processamento na camada de negócio, além de onerar a rede com chamadas de API inválidas para usuários sem fundos.

## Causa raiz
1. **Falta de isolamento e acoplamento de responsabilidades:** O fluxo de pagamentos não realizava uma verificação prévia (*Fail-Fast*) na carteira do usuário antes de disparar a requisição de rede para a API de adquirência externa.
2. **Ausência de proteção de resiliência:** Chamadas HTTP eram feitas de forma direta via `RestTemplate`, deixando as Threads do ecossistema expostas a timeouts e lentidões do parceiro terceirizado.
3. **Modelagem Anêmica:** As regras de alteração de saldo financeiro não estavam blindadas na entidade, permitindo manipulação inconsistente do estado do recurso.

## Investigação
A análise do código-fonte do `PaymentGatewayService` revelou que a ordem de execução priorizava a chamada externa HTTP via `restTemplate.postForEntity` antes de efetuar qualquer validação de segurança do saldo do torcedor. Constatou-se também que os valores monetários estavam expostos a falhas de arredondamento de ponto flutuante caso fossem utilizados tipos primitivos, exigindo a substituição e a padronização rigorosa para `BigDecimal`.

## Conceito estudado
* **Orquestração e Fail-Fast:** Validação precoce de pré-requisitos antes do consumo de recursos de infraestrutura externos.
* **Circuit Breaker (Disjuntor de Resiliência):** Máquina de estados (Closed, Open, Half-Open) fornecida pelo *Resilience4j* para isolar falhas de integração e executar fluxos alternativos (*Fallback*).
* **Rich Domain Model:** Centralização das regras de mutação de estado financeiro dentro da própria entidade de domínio para proteção de integridade.

## Solução
1. **Implementação do Domínio Rich `Wallet`:** Criação da entidade `Wallet` com encapsulamento estrito dos métodos `.debit()` e `.credit()`, utilizando validações com `BigDecimal.compareTo()`.
2. **Desenvolvimento do `WalletService`:** Criação da camada de negócio transacional (`@Transactional`) para controle de depósitos, débitos e o método especialista `.validateBalance()`.
3. **Refatoração do `PaymentGatewayService`:** Nova ordem de execução orquestrada em etapas protegidas:
    * **Passo 1:** Executa `walletService.validateBalance()`. Se falhar, aborta imediatamente.
    * **Passo 2:** Dispara a chamada com a anotação `@CircuitBreaker(name = "pagBrasilPay")`.
    * **Passo 3 (Fluxo de Sucesso):** Com a confirmação da API, executa `walletService.debit()`.
    * **Passo 4 (Fluxo de Fallback):** Caso o disjuntor abra ou a API falhe, intercepta o erro, o saldo do cliente fica **intacto** (não debita) e o pagamento é salvo como `PaymentStatus.PENDING`.
4. **Exposição RESTful:** Criação do `WalletController` mapeando rotas baseadas nos verbos HTTP adequados e status codes corporativos (`201 Created` e `200 OK`).

## Arquitetura antes
```text
[Client Request] ──► PaymentGatewayService ──► [API Externa pagBrasilPay] ──► (Débito Direto sem Validação)
```

## Arquitetura depois
```text
  [Request] 
      │
      ▼
┌───────────────────────────┐
│ 1. WalletService          │ ──► [Saldo Insuficiente?] ──► (Lança Exceção / Aborta)
│    .validateBalance()     │
└───────────────────────────┘
      │ (Saldo OK)
      ▼
┌───────────────────────────┐
│ 2. Circuit Breaker        │ ──► [API Fora do Ar / Aberto] ──► (Executa Fallback)
│    (Resilience4j)         │                                         │
└───────────────────────────┘                                         ▼
      │ (Disjuntor Fechado)                                  ┌──────────────────────────┐
      ▼                                                      │ Salva como PENDING       │
┌───────────────────────────┐                                │ Saldo do cliente intacto │
│ 3. RestTemplate           │                                └──────────────────────────┘
│    POST /payments         │
└───────────────────────────┘
      │ (Sucesso 200 OK)
      ▼
┌───────────────────────────┐
│ 4. WalletService          │
│    .debit() + Save        │
└───────────────────────────┘
```

## Trade-offs
* **Abordagem Escolhida: Débito Postergado (Após confirmação da Adquirência):**
    * *Pró:* Segurança integral do saldo do usuário. Elimina a necessidade de estornos operacionais manuais complexos em caso de falha pós-débito.
    * *Contra:* Em ambientes de altíssima concorrência simultânea em milissegundos idênticos (*Race Condition*), a validação em memória do Java pode ler dados desatualizados antes da persistência do primeiro débito.
* **Banco em Memória H2 (`ddl-auto: create-drop`):**
    * *Pró:* Isolamento completo e velocidade total para a validação das regras de negócio nesta fase inicial de laboratório.
    * *Contra:* Volatilidade absoluta dos dados, exigindo migração para banco relacional estável e evolutivo na próxima fase.

## Testes
Foi desenvolvida uma suíte de testes unitários isolados com **JUnit 5** e **Mockito** na classe `WalletServiceTest`. Foram validados os seguintes comportamentos programados por meio de dublês de teste (`@Mock` e `@InjectMocks`):
1. Arremesso de `IllegalArgumentException` ao buscar chaves de carteiras inexistentes.
2. Dedução correta do saldo e acionamento obrigatório do método `.save()` em cenários de débito válido.
3. Bloqueio absoluto com lançamento de `IllegalStateException` em casos de fundos insuficientes, validando que o repositório nunca invoque alterações de dados (`verify(..., never()).save(...)`).
4. Rejeição de aportes financeiros de valor zerado ou negativo no fluxo de depósitos.
5. Garantia de passagem limpa sem o lançamento de erros no método `validateBalance` quando as condições financeiras estão saudáveis.

## Como reproduzir
1. Certifique-se de possuir o Java 17+ instalado na máquina.
2. Configure o arquivo `src/main/resources/application.yml` adicionando o bloco `spring.datasource` apontando para o driver `org.h2.Driver` e a URL `jdbc:h2:mem:footbank`.
3. Execute a aplicação Spring Boot através do comando `./mvnw spring-boot:run` ou pela IDE.
4. Para realizar os testes de validação lógica e mocks, execute o comando:
   ```bash
   mvn test
   ```
5. Para inspecionar visualmente o estado inicial das tabelas geradas dinamicamente, acesse `http://localhost:8080/h2-console` no navegador enquanto a aplicação estiver em execução.

## Lições aprendidas
1. **Blindagem do Domínio:** As entidades de negócio nunca devem ser anêmicas. Centralizar as operações matemáticas de débito e crédito no próprio objeto `Wallet` assegura que o estado mude apenas sob validações estritas da regra corporativa.
2. **Arquitetura Orientada à Resiliência:** Sistemas distribuídos falham de maneira imprevisível. Isolar pontos de integração vulneráveis usando padrões de Circuit Breaker protege as Threads do ecossistema principal e mantém a plataforma estável.
