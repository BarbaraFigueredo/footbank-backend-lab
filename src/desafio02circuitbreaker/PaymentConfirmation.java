package com.footbank.desafio02circuitbreaker;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentConfirmation(
    String transactionId,
    String status,
    BigDecimal amount,
    Instant processedAt
) {}
