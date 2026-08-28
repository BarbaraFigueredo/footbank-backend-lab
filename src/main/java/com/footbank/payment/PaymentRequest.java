package com.footbank.payment;

import java.math.BigDecimal;

public record PaymentRequest(
    String walletId,
    BigDecimal amount,
    String description
) {}
