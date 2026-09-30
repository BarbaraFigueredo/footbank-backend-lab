package com.footbank.wallet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private TransferService transferService;

    @Test
    @DisplayName("1. Deve debitar da origem e creditar no destino em uma transferência válida")
    void deveTransferirComSucesso() {
        TransferRequest request = new TransferRequest(
                "wallet-carlos", "wallet-pedro", new BigDecimal("150.00"), "Ingresso final"
        );

        TransferResult result = transferService.transfer(request);

        verify(walletService).validateBalance("wallet-carlos", new BigDecimal("150.00"));
        verify(walletService).debit("wallet-carlos", new BigDecimal("150.00"));
        verify(walletService).deposit("wallet-pedro", new BigDecimal("150.00"));

        assertEquals("wallet-carlos", result.sourceWalletId());
        assertEquals("wallet-pedro", result.targetWalletId());
        assertEquals(new BigDecimal("150.00"), result.amount());
        assertNotNull(result.transferId());
        assertNotNull(result.transferredAt());
    }

    @Test
    @DisplayName("2. Deve lançar exceção e não movimentar nenhuma carteira quando o saldo for insuficiente")
    void deveLancarExcecaoQuandoSaldoInsuficiente() {
        TransferRequest request = new TransferRequest(
                "wallet-carlos", "wallet-pedro", new BigDecimal("500.00"), "Ingresso"
        );
        doThrow(new IllegalStateException("Saldo insuficiente na carteira..."))
                .when(walletService).validateBalance("wallet-carlos", new BigDecimal("500.00"));

        assertThrows(IllegalStateException.class, () -> transferService.transfer(request));

        verify(walletService, never()).debit(any(), any());
        verify(walletService, never()).deposit(any(), any());
    }

    @Test
    @DisplayName("3. Deve lançar exceção sem tocar em nenhuma carteira quando origem e destino forem iguais")
    void deveLancarExcecaoParaMesmaCarteira() {
        TransferRequest request = new TransferRequest(
                "wallet-carlos", "wallet-carlos", new BigDecimal("100.00"), "Transferência inválida"
        );

        assertThrows(IllegalArgumentException.class, () -> transferService.transfer(request));

        verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("4. Deve lançar exceção sem tocar em nenhuma carteira quando o valor for zero ou negativo")
    void deveLancarExcecaoParaValorInvalido() {
        TransferRequest requestZero = new TransferRequest(
                "wallet-carlos", "wallet-pedro", BigDecimal.ZERO, "Ingresso"
        );
        TransferRequest requestNegativo = new TransferRequest(
                "wallet-carlos", "wallet-pedro", new BigDecimal("-50.00"), "Ingresso"
        );

        assertThrows(IllegalArgumentException.class, () -> transferService.transfer(requestZero));
        assertThrows(IllegalArgumentException.class, () -> transferService.transfer(requestNegativo));

        verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("5. Deve lançar exceção quando a carteira de origem não existir")
    void deveLancarExcecaoQuandoCarteiraOrigemNaoExiste() {
        TransferRequest request = new TransferRequest(
                "wallet-inexistente", "wallet-pedro", new BigDecimal("100.00"), "Ingresso"
        );
        doThrow(new IllegalArgumentException("Carteira wallet-inexistente não encontrada."))
                .when(walletService).validateBalance("wallet-inexistente", new BigDecimal("100.00"));

        assertThrows(IllegalArgumentException.class, () -> transferService.transfer(request));

        verify(walletService, never()).debit(any(), any());
        verify(walletService, never()).deposit(any(), any());
    }
}
