# 002 — Transferência entre Carteiras

## Contexto

O FootBank possui um domínio de carteiras digitais com operações de depósito, débito e consulta de saldo. Torcedores passaram a solicitar a possibilidade de transferir saldo entre si — por exemplo, um familiar pagando o ingresso de outro.

## Requisito

```
POST /api/wallets/transfer
{
  "sourceWalletId": "wallet-carlos",
  "targetWalletId": "wallet-pedro",
  "amount": 150.00,
  "description": "Ingresso Flamengo x Santos — Setor Norte"
}
```

## Objetivo

Implementar a transferência de saldo entre duas carteiras de forma **atômica**: ou os dois saldos são atualizados juntos, ou nenhum é.

## Conceito estudado

**Atomicidade em operações multi-entidade**

Uma transferência envolve dois registros distintos no banco. Se a operação for interrompida após o débito mas antes do crédito, o dinheiro some. A solução é garantir que os dois updates ocorram dentro da **mesma transação de banco de dados**.

No Spring, isso é feito com `@Transactional` no método de serviço. O commit só acontece ao final do método. Qualquer exceção durante a execução dispara um rollback automático — desfazendo todas as mudanças da transação, incluindo o débito já realizado.

**Propagação de transação (`REQUIRED`)**

Os métodos `WalletService.debit()` e `WalletService.deposit()` também são `@Transactional`. Quando chamados a partir de `TransferService.transfer()` (que já abriu uma transação), eles participam da transação existente em vez de abrir uma nova. Isso é o comportamento padrão `REQUIRED` do Spring.

## Fluxo de execução

```
POST /api/wallets/transfer
         ↓
TransferController.transfer()
         ↓
TransferService.transfer()
    ↓ [TRANSAÇÃO ABERTA]
    1. validar amount > 0
    2. validar source != target
    3. walletService.validateBalance(source, amount)  ← lança exceção se saldo insuficiente
    4. walletService.debit(source, amount)            ← debita carteira origem
    5. walletService.deposit(target, amount)          ← credita carteira destino
    ↓ [COMMIT — ambas as mudanças persistidas]
    6. retorna TransferResult com transferId e timestamp
```

## Arquitetura

```
TransferController
        ↓ delega
TransferService (@Transactional)
        ↓ reutiliza
WalletService
    debit()     → Wallet.debit()     → walletRepository.save()
    deposit()   → Wallet.credit()    → walletRepository.save()
```

`TransferService` não acessa o repositório diretamente — orquestra operações através do `WalletService`, respeitando as camadas e reutilizando as regras de negócio já encapsuladas.

## Classes criadas

| Classe | Responsabilidade |
|---|---|
| `TransferRequest` | Record com os dados da requisição (sourceWalletId, targetWalletId, amount, description) |
| `TransferResult` | Record de resposta com transferId, wallets, valor e timestamp |
| `TransferService` | Orquestra a transferência atômica via WalletService |
| `TransferController` | Expõe `POST /api/wallets/transfer`, delega ao TransferService |

## Validações

| Regra | Exceção |
|---|---|
| `amount` nulo ou <= 0 | `IllegalArgumentException` |
| `sourceWalletId == targetWalletId` | `IllegalArgumentException` |
| Saldo insuficiente | `IllegalStateException` (via WalletService) |
| Carteira não encontrada | `IllegalArgumentException` (via WalletService) |

## Testes

Cinco cenários protegidos em `TransferServiceTest`:

1. **Transferência válida** — verifica que `validateBalance`, `debit` e `deposit` foram chamados com os argumentos corretos e que o `TransferResult` está completo
2. **Saldo insuficiente** — `validateBalance` lança exceção; `debit` e `deposit` nunca são chamados
3. **Mesma carteira** — validação acontece antes de qualquer chamada ao `WalletService`
4. **Valor inválido (zero ou negativo)** — nenhuma operação de carteira é executada
5. **Carteira inexistente** — exceção sobe do `validateBalance`; `debit` nunca é chamado

## Trade-offs

| Decisão | Vantagem | Desvantagem |
|---|---|---|
| `@Transactional` no TransferService | Atomicidade garantida pelo Spring/JPA | Requer que as operações estejam na mesma fonte de dados |
| Reutilizar `WalletService` | Sem duplicação de lógica, regras de negócio centralizadas | Dependência entre serviços do mesmo domínio |
| `deposit()` para creditar a carteira destino | Reutiliza código existente | Semântica imprecisa: `deposit` implica origem externa; transferência é operação interna |

## Observação de design

O `WalletService` expõe `debit()` e `deposit()` — assimétrico em relação à linguagem da entidade `Wallet`, que usa `debit()` e `credit()`. Uma evolução futura seria adicionar `WalletService.credit()` para separar "crédito por transferência interna" de "depósito de origem externa", alinhando vocabulário de serviço com vocabulário de domínio.

## Lições aprendidas

- Atomicidade em operações multi-entidade é garantida por uma única transação envolvendo todos os passos
- A propagação `REQUIRED` do Spring elimina a necessidade de coordenação manual entre `@Transactional` aninhados
- Controllers não validam lógica de negócio — delegam ao service e devolvem o resultado como resposta HTTP
- `verifyNoInteractions(mock)` em testes é uma garantia de arquitetura: nenhuma operação colateral aconteceu antes da exceção
