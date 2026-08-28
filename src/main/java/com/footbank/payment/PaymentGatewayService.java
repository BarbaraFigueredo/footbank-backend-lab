package com.footbank.payment;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentGatewayService {

    private final RestTemplate restTemplate;
    private final String gatewayUrl;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    public PaymentGatewayService(RestTemplate restTemplate,
                                  @Value("${gateway.url}") String gatewayUrl,
                                  PaymentRepository paymentRepository,
                                  NotificationService notificationService) {
        this.restTemplate = restTemplate;
        this.gatewayUrl = gatewayUrl;
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
    }

    @CircuitBreaker(name = "pagBrasilPay", fallbackMethod = "processPaymentFallback")
    public PaymentConfirmation processPayment(PaymentRequest request) {
        ResponseEntity<PaymentConfirmation> response = restTemplate.postForEntity(
            gatewayUrl + "/payments",
            request,
            PaymentConfirmation.class
        );
        return response.getBody();
    }

    // Assinatura obrigatória: mesmos parâmetros + Throwable no final
    private PaymentConfirmation processPaymentFallback(PaymentRequest request, Throwable ex) {
        String transactionId = UUID.randomUUID().toString();

        paymentRepository.save(new Payment(transactionId, request, PaymentStatus.PENDING));
        notificationService.sendPendingPaymentEmail(request.walletId(), transactionId);

        return new PaymentConfirmation(
            transactionId,
            "PENDING",
            request.amount(),
            Instant.now()
        );
    }
}
