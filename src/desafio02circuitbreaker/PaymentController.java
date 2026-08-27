package com.footbank.desafio02circuitbreaker;

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

    // PROBLEMA: nenhum tratamento de erro ou fallback aqui.
    // Se o gateway falhar, o usuário recebe um erro 500 genérico
    // e o serviço continua tentando chamar um gateway que está morto.
    //
    // TODO: Como o controller deve reagir quando o Circuit Breaker estiver aberto?
    //       Que HTTP status e mensagem fazem sentido para o cliente de uma fintech?
    @PostMapping
    public ResponseEntity<PaymentConfirmation> pay(@RequestBody PaymentRequest request) {
        PaymentConfirmation confirmation = gatewayService.processPayment(request);
        return ResponseEntity.ok(confirmation);
    }
}
