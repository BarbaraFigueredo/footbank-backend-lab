package com.footbank.wallet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// aqui vamos ativar a extensão do mockito
// para as anotações funcionarem
@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    // cria o repositório falso
    @Mock
    private WalletRepository walletRepository;

    // injeta o repositorio falso automaticamente dentro do serviço
    @InjectMocks
    private WalletService walletService;

    @Test
    @DisplayName("1. Deve lançar exceção quando a carteira não for encontrada no findById")
    void deveLancarExcecaoQuandoCarteiraNaoEncontrada() {
        // given (dado que) programamos para retornar vazio (Optional.empty)
        when(walletRepository.findById("id-inexistente")).thenReturn(Optional.empty());

        // when e then (quando e entao) verificamos se ele joga a exceção na tela
        assertThrows(IllegalArgumentException.class, () -> {
            walletService.findById("id-inexistente");
        });
    }

    @Test
    @DisplayName("2. Deve reduzir o balance e chamar o save quando houver saldo suficiente no débito")
    void deveDebitarComSaldoSuficiente() {
        // given - criamos uma carteira real e colocamos saldo nela usando crédito
        Wallet wallet = new Wallet("id-123", "owner-1");
        wallet.credit(new BigDecimal("100.00")); // saldo atual 100.00

        when(walletRepository.findById("id-123")).thenReturn(Optional.of(wallet));

        // when - executamos o débito de 40.00
        walletService.debit("id-123", new BigDecimal("40.00"));

        // then - conferimos as contas matemáticas e se o banco foi acionado
        assertEquals(new BigDecimal("60.00"), wallet.getBalance()); // 100 - 40 = 60
        verify(walletRepository).save(wallet); // garante que o método save() foi chamado
    }

    @Test
    @DisplayName("3. Deve lançar IllegalStateException no débito quando o saldo for insuficiente")
    void deveLancarExcecaoNoDebitComSaldoInsuficiente() {
        // given carteira nasce com saldo ZERO
        Wallet wallet = new Wallet("id-123", "owner-1");
        when(walletRepository.findById("id-123")).thenReturn(Optional.of(wallet));

        // when e then tentar debitar 50.00 de onde tem 0.00 deve falhar
        assertThrows(IllegalStateException.class, () -> {
            walletService.debit("id-123", new BigDecimal("50.00"));
        });

        // garantia de arquitetura: o banco NUNCA deve ser atualizado se a transação falhou
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("4. Deve aumentar o balance e chamar o save no depósito válido")
    void deveDepositarComValorValido() {
        // given
        Wallet wallet = new Wallet("id-123", "owner-1"); // saldo 0.00
        when(walletRepository.findById("id-123")).thenReturn(Optional.of(wallet));

        // when
        walletService.deposit("id-123", new BigDecimal("50.00"));

        // then
        assertEquals(new BigDecimal("50.00"), wallet.getBalance());
        verify(walletRepository).save(wallet);
    }

    @Test
    @DisplayName("5. Deve lançar IllegalStateException no depósito quando o valor for zero ou negativo")
    void deveLancarExcecaoNoDepositComValorInvalido() {
        // given
        Wallet wallet = new Wallet("id-123", "owner-1");
        when(walletRepository.findById("id-123")).thenReturn(Optional.of(wallet));

        // when e then testando valor negativo
        assertThrows(IllegalStateException.class, () -> {
            walletService.deposit("id-123", new BigDecimal("-10.00"));
        });
    }

    @Test
    @DisplayName("6. Não deve lançar exceção no validateBalance se o saldo estiver ok")
    void deveValidarBalançoComSaldoOk() {
        // given
        Wallet wallet = new Wallet("id-123", "owner-1");
        wallet.credit(new BigDecimal("100.00"));
        when(walletRepository.findById("id-123")).thenReturn(Optional.of(wallet));

        // when e then assertDoesNotThrow garante que o código passou liso sem nenhum erro
        assertDoesNotThrow(() -> {
            walletService.validateBalance("id-123", new BigDecimal("50.00"));
        });
    }

    @Test
    @DisplayName("7. Deve lançar IllegalStateException no validateBalance se não houver saldo")
    void deveLancarExcecaoNoValidateBalanceSemSaldo() {
        // given
        Wallet wallet = new Wallet("id-123", "owner-1"); // saldo 0.00
        when(walletRepository.findById("id-123")).thenReturn(Optional.of(wallet));

        // when e then
        assertThrows(IllegalStateException.class, () -> {
            walletService.validateBalance("id-123", new BigDecimal("10.00"));
        });
    }
}