package com.footbank.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentGatewayService gatewayService;

    public PaymentController(PaymentGatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @PostMapping
    public ResponseEntity<PaymentConfirmation> pay(@RequestBody PaymentRequest request) {
        PaymentConfirmation confirmation = gatewayService.processPayment(request);

        // 202 Accepted quando o pagamento está pendente por indisponibilidade do gateway
        if ("PENDING".equals(confirmation.status())) {
            return ResponseEntity.accepted().body(confirmation);
        }

        return ResponseEntity.ok(confirmation);
    }
}
