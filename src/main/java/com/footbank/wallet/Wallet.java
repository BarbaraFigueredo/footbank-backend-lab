package com.footbank.wallet;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallets")
public class Wallet {
    @Id
    private String walletId;

    private String ownerId;
    @Version
    private Long version;
    private BigDecimal balance;
    private Instant createdAt;

    protected Wallet() {
    }

    public Wallet(String walletId, String ownerId) {
        this.walletId = walletId;
        this.ownerId = ownerId;
        this.balance = BigDecimal.ZERO;
        this.createdAt = Instant.now();
    }

    public String getWalletId() {
        return walletId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) >= 0) {
            this.balance = this.balance.subtract(amount);
        } else {
            throw new IllegalStateException("Saldo insuficiente");
        }
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Valor de crédito inválido");
        }
        this.balance = this.balance.add(amount);
    }
}