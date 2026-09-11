package com.footbank.payment;

import com.footbank.wallet.Wallet;
import com.footbank.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private PaymentRepository paymentRepository;
    @Mock private NotificationService notificationService;
    @Mock private WalletService walletService;

    private PaymentGatewayService service;

    private final PaymentRequest request = new PaymentRequest("wallet-carlos", new BigDecimal("200.00"), "Ingresso Norte");
    private final PaymentConfirmation gatewayConfirmation = new PaymentConfirmation("txn-abc", "APPROVED", new BigDecimal("200.00"), Instant.now());

    @BeforeEach
    void setUp() {
        service = new PaymentGatewayService(
            restTemplate,
            "http://fake-gateway",
            paymentRepository,
            notificationService,
            walletService
        );
    }

    @Test
    @DisplayName("1. Deve completar o pagamento normalmente quando não há concorrência")
    void deveCompletarPagamentoSemConcorrencia() {
        when(restTemplate.postForEntity(anyString(), any(), eq(PaymentConfirmation.class)))
            .thenReturn(ResponseEntity.ok(gatewayConfirmation));

        PaymentConfirmation result = service.processPayment(request);

        verify(walletService).validateBalance("wallet-carlos", new BigDecimal("200.00"));
        verify(walletService, times(1)).debit("wallet-carlos", new BigDecimal("200.00"));
        assertEquals("APPROVED", result.status());
        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("2. Deve debitar na segunda tentativa quando a primeira falha por conflito de concorrência")
    void deveDebitarNaSegundaTentativaAposOptimisticLock() {
        when(restTemplate.postForEntity(anyString(), any(), eq(PaymentConfirmation.class)))
            .thenReturn(ResponseEntity.ok(gatewayConfirmation));
        when(walletService.debit(eq("wallet-carlos"), any()))
            .thenThrow(new ObjectOptimisticLockingFailureException(Wallet.class, "wallet-carlos"))
            .thenReturn(mock(Wallet.class));

        PaymentConfirmation result = service.processPayment(request);

        // o sistema tentou debitar duas vezes
        verify(walletService, times(2)).debit("wallet-carlos", new BigDecimal("200.00"));
        // pagamento concluído normalmente — sem PENDING
        assertEquals("APPROVED", result.status());
        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("3. Deve salvar como PENDING e notificar quando o saldo acaba entre a primeira e a segunda tentativa")
    void deveRetornarPendingQuandoSaldoEsgotadoEntreAsTentativas() {
        when(restTemplate.postForEntity(anyString(), any(), eq(PaymentConfirmation.class)))
            .thenReturn(ResponseEntity.ok(gatewayConfirmation));
        when(walletService.debit(eq("wallet-carlos"), any()))
            .thenThrow(new ObjectOptimisticLockingFailureException(Wallet.class, "wallet-carlos"))
            .thenThrow(new IllegalStateException("Saldo insuficiente"));

        PaymentConfirmation result = service.processPayment(request);

        // gateway aprovou, mas não foi possível debitar — fica PENDING
        assertEquals("PENDING", result.status());
        // pagamento deve ser persistido para reprocessamento
        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.PENDING));
        // cliente deve ser avisado
        verify(notificationService).sendPendingPaymentEmail("wallet-carlos", "txn-abc");
    }
}
