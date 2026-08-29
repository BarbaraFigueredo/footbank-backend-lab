package com.footbank.wallet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository){
        this.walletRepository = walletRepository;
    }

    @Transactional
    public Wallet create(String ownerId){
        String newWalletId = UUID.randomUUID().toString();

        Wallet newWallet = new Wallet(newWalletId, ownerId);
        return walletRepository.save(newWallet);
    }

    public Wallet findById(String walletId){
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Carteira " + walletId + " não encontrada."));
    }

    @Transactional
    public Wallet deposit(String walletId, BigDecimal amount){
        Wallet wallet = findById(walletId);
        wallet.credit(amount);
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet debit(String walletId, BigDecimal amount){
        Wallet wallet = findById(walletId);
        wallet.debit(amount);
        return walletRepository.save(wallet);
    }

    public void validateBalance(String walletId, BigDecimal amount){
        Wallet wallet = findById(walletId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Saldo insuficiente na carteira...");
        }
    }
}