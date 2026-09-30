package com.footbank.wallet;

import java.math.BigDecimal;

public record TransferRequest(
        String sourceWalletId,
        String targetWalletId,
        BigDecimal amount,
        String description
) {}