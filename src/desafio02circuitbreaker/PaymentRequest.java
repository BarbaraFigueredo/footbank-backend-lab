package com.footbank.desafio02circuitbreaker;

import java.math.BigDecimal;

public record PaymentRequest(
    String walletId,
    BigDecimal amount,
    String description
) {}
