package com.footbank.wallet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class TransferService {

    private final WalletService walletService;

    public TransferService(WalletService walletService) {
        this.walletService = walletService;
    }

    @Transactional
    public TransferResult transfer(TransferRequest request) {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da transferência deve ser maior que zero.");
        }
        if (request.sourceWalletId().equals(request.targetWalletId())) {
            throw new IllegalArgumentException("Não é possível transferir para a mesma carteira.");
        }

        walletService.validateBalance(request.sourceWalletId(), request.amount());
        walletService.debit(request.sourceWalletId(), request.amount());
        walletService.deposit(request.targetWalletId(), request.amount());

        return new TransferResult(
                UUID.randomUUID().toString(),
                request.sourceWalletId(),
                request.targetWalletId(),
                request.amount(),
                Instant.now()
        );
    }
}
