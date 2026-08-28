package com.footbank.payment;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private String transactionId;

    private String walletId;
    private BigDecimal amount;
    private String description;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private Instant createdAt;

    protected Payment() {}

    public Payment(String transactionId, PaymentRequest request, PaymentStatus status) {
        this.transactionId = transactionId;
        this.walletId = request.walletId();
        this.amount = request.amount();
        this.description = request.description();
        this.status = status;
        this.createdAt = Instant.now();
    }

    public String getTransactionId() { return transactionId; }
    public String getWalletId() { return walletId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
