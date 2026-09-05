package com.footbank.wallet;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallets")
public class WalletController{

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<Wallet> criarCarteira(@RequestBody CreateWalletRequest request){
        Wallet newWallet = walletService.create(request.ownerId());

        return ResponseEntity.status(HttpStatus.CREATED).body(newWallet);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Wallet> consultarSaldo(@PathVariable("id") String walletId){
        Wallet wallet = walletService.findById(walletId);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<Wallet> depositar(@PathVariable("id") String walletId, @RequestBody AmountRequest request){
        Wallet walletAtualizada = walletService.deposit(walletId, request.amount());
        return ResponseEntity.ok(walletAtualizada);
    }

    @PostMapping("/{id}/debit")
    public ResponseEntity<Wallet> debitar(@PathVariable("id") String walletId, @RequestBody AmountRequest request){
        Wallet walletAtualizada = walletService.debit(walletId, request.amount());
        return ResponseEntity.ok(walletAtualizada);
    }

    // DTOs (objetos auxiliares para receber os dados JSON da internet

    record CreateWalletRequest(String ownerId) {}
    record AmountRequest(BigDecimal amount) {}
}