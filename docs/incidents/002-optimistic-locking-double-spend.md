# 002 — Double Spend: Torcedor Gastou Mais do que Tinha

## Contexto

O `payment-service` integra com o gateway externo PagBrasilPay para processar pagamentos de ingressos. A carteira digital (`wallet-service`) é responsável por controlar o saldo de cada torcedor. Durante a final do Brasileirão, requisições simultâneas passaram a atingir a mesma carteira.

## Problema

Um torcedor com saldo de R$ 200,00 conseguiu processar dois pagamentos de R$ 200,00 ao mesmo tempo. Três casos foram confirmados pela equipe financeira após o evento.

## Impacto

- 3 torcedores identificados com saldo inconsistente
- Prejuízo estimado: R$ 1.800,00
- Incidente reportado às 22h17 durante a final do Brasileirão

## Causa raiz

**Lost Update por ausência de Optimistic Locking na entidade `Wallet`.**

O fluxo de pagamento envolve três operações separadas:

```
validateBalance  →  gateway externo  →  debit
(transação 1)       (fora de tx)       (transação 2)
```

Duas requisições concorrentes passam pela `validateBalance` antes de qualquer commit. Ambas enxergam o mesmo saldo. Ambas chamam o gateway com sucesso. Quando executam o `debit`, a segunda transação lê a versão antiga da carteira (antes do commit da primeira) e sobrescreve o saldo como se a primeira nunca tivesse existido.

## Investigação

```
Carlos: saldo R$ 200,00

t=0ms   A: validateBalance → lê 200 ✓
t=0ms   B: validateBalance → lê 200 ✓
t=5ms   A: gateway → APPROVED
t=5ms   B: gateway → APPROVED
t=50ms  A: debit → findById → 200, subtrai 200, salva 0, commit
t=50ms  B: debit → findById → 200 (leu antes do commit de A!)
t=51ms  B: subtrai 200, salva 0, commit → sobrescreve A
```

Resultado: dois ingressos comprados, carteira debitada apenas uma vez.

## Conceito estudado

**Optimistic Locking com `@Version`**

Em vez de bloquear o registro no banco durante toda a operação (Pessimistic Locking), o Optimistic Locking permite que múltiplas transações leiam o mesmo dado ao mesmo tempo. A proteção acontece no momento do UPDATE:

```sql
UPDATE wallets
SET balance = 0, version = 8
WHERE id = 'carlos' AND version = 7
```

Se o `version` mudou desde a leitura, o banco retorna `0 rows affected`. O JPA detecta isso e lança `ObjectOptimisticLockingFailureException`. A transação é rejeitada antes de causar inconsistência.

## Solução

**1. Campo `@Version` adicionado à entidade `Wallet`:**

```java
@Version
private Long version;
```

O JPA gerencia esse campo automaticamente — incrementa a cada UPDATE e inclui a versão na cláusula WHERE.

**2. Retry com fallback para PENDING em `PaymentGatewayService`:**

```
OptimisticLockException capturada
        ↓
Segunda tentativa de debit (dados frescos do banco)
        ↓
    Sucesso → retorna APPROVED
        ↓
    IllegalStateException (saldo esgotado) → salva PENDING + notifica
```

Como o gateway já havia aprovado o pagamento, não podemos simplesmente retornar erro. A segunda tentativa é obrigatória para honrar a aprovação.

## Arquitetura antes

```
PaymentGatewayService
        ↓
walletService.validateBalance()
        ↓
PagBrasilPay API
        ↓
walletService.debit()     ← sem proteção contra concorrência
```

## Arquitetura depois

```
PaymentGatewayService
        ↓
walletService.validateBalance()
        ↓
PagBrasilPay API
        ↓
walletService.debit()     ← [version=N no UPDATE]
        ↓ OptimisticLockException
retry → walletService.debit()   ← lê version atualizado
        ↓ IllegalStateException
salva PENDING + notifica
```

## Trade-offs

| Decisão | Vantagem | Desvantagem |
|---|---|---|
| Optimistic Locking | Sem bloqueio, alta performance | Requer tratamento de exceção e retry |
| Retry único | Resolve a maioria dos casos reais | Não cobre dois conflitos consecutivos (extremamente raro) |
| Fallback PENDING | Pagamento aprovado pelo gateway não se perde | Precisa de reprocessamento posterior |
| Pessimistic Locking (não adotado) | Garante exclusão total | Bloqueia threads, degrada performance em alta concorrência |

## Testes

Três cenários protegidos em `PaymentGatewayServiceTest`:

1. **Sem concorrência** — `debit` chamado uma vez, resultado `APPROVED`
2. **Conflito resolvido no retry** — primeira tentativa falha com `OptimisticLockException`, segunda tem sucesso, resultado `APPROVED`
3. **Saldo esgotado entre as tentativas** — segunda tentativa lança `IllegalStateException`, sistema salva `PENDING` e notifica o torcedor

## Lições aprendidas

- A validação de saldo e o débito em transações separadas criam uma janela de vulnerabilidade — qualquer operação externa entre elas amplia esse risco
- Optimistic Locking é a proteção padrão do JPA para esse padrão de acesso concorrente
- Em sistemas financeiros, quando o gateway já aprovou, não podemos simplesmente descartar — o retry ou o PENDING preservam a integridade
- Testes unitários com Mockito conseguem simular a sequência de exceções (`.thenThrow().thenReturn()`) sem precisar de banco ou threads reais
