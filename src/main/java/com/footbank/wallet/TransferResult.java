package com.footbank.wallet;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResult(
        String transferId,
        String sourceWalletId,
        String targetWalletId,
        BigDecimal amount,
        Instant transferredAt
) {}
