package com.footbank.desafio02circuitbreaker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PaymentGatewayService {

    private final RestTemplate restTemplate;
    private final String gatewayUrl;

    public PaymentGatewayService(RestTemplate restTemplate,
                                  @Value("${gateway.url}") String gatewayUrl) {
        this.restTemplate = restTemplate;
        this.gatewayUrl = gatewayUrl;
    }

    // PROBLEMA: chamada direta ao gateway externo sem nenhuma proteção.
    // Quando o PagBrasilPay fica lento ou cai, todas as threads ficam bloqueadas
    // aqui, causando cascata de falhas nos outros serviços.
    //
    // TODO: Adicione um Circuit Breaker neste método.
    //       - O que deve acontecer quando o gateway começar a falhar?
    //       - O que retornar para o cliente enquanto o circuito estiver aberto?
    //       - Qual anotação do Resilience4j você usaria aqui?
    public PaymentConfirmation processPayment(PaymentRequest request) {
        ResponseEntity<PaymentConfirmation> response = restTemplate.postForEntity(
            gatewayUrl + "/payments",
            request,
            PaymentConfirmation.class
        );
        return response.getBody();
    }
}
