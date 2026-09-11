package com.footbank.payment;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.footbank.wallet.WalletService;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentGatewayService {

    private final RestTemplate restTemplate;
    private final String gatewayUrl;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;


    // [1] Passo Novo: Declaramos a nossa dependência do gerenciador de carteiras
    private final WalletService walletService;

    // Ajustamos o construtor para receber o WalletService (Injeção de Dependência padrão produção)
    public PaymentGatewayService(RestTemplate restTemplate,
                                  @Value("${gateway.url}") String gatewayUrl,
                                  PaymentRepository paymentRepository,
                                  NotificationService notificationService,
                                 WalletService walletService) {
        this.restTemplate = restTemplate;
        this.gatewayUrl = gatewayUrl;
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
        this.walletService = walletService;
    }

    @CircuitBreaker(name = "pagBrasilPay", fallbackMethod = "processPaymentFallback")
    public PaymentConfirmation processPayment(PaymentRequest request) {

        // [2] REGRA ANTES DO GATEWAY: Validar o saldo antes de tentar cobrar!
        // Se o torcedor não tiver saldo, o método lança uma exceção e o código nem chega na API externa.
        walletService.validateBalance(request.walletId(), request.amount());

        ResponseEntity<PaymentConfirmation> response = restTemplate.postForEntity(
            gatewayUrl + "/payments",
            request,
            PaymentConfirmation.class
        );

        // Se a chamada acima deu certo e não jogou nenhum erro, a confirmação chegou!
        PaymentConfirmation confirmation = response.getBody();

        try {
            // [3] PRIMEIRA TENTATIVA DE DÉBITO
            walletService.debit(request.walletId(), request.amount());

        } catch (ObjectOptimisticLockingFailureException ex) {
            //  Alguém alterou a carteira no mesmo milissegundo
            System.out.println("Concorrência detectada! Tentando novamente com dados frescos...");

            try {
                // [TENTATIVA 2] Chamamos o débito novamente
                // O walletService.debit internamente já busca a carteira de novo
                // trazendo a versão atualizada do banco
                walletService.debit(request.walletId(), request.amount());

            } catch (IllegalStateException e) {
                // Se na segunda tentativa der erro de saldo insuficiente,
                // precisamos salvar como PENDING e notificar igual fazemos no Fallback

                String transactionId = confirmation.transactionId(); // ou gerar um UUID
                paymentRepository.save(new Payment(transactionId, request, PaymentStatus.PENDING));
                notificationService.sendPendingPaymentEmail(request.walletId(), transactionId);

                // Ajustamos a confirmação para avisar que ficou pendente
                return new PaymentConfirmation(transactionId, "PENDING", request.amount(), Instant.now());
            }
        }


        return confirmation;
    }

    // Assinatura obrigatória: mesmos parâmetros + Throwable no final
    private PaymentConfirmation processPaymentFallback(PaymentRequest request, Throwable ex) {
        System.out.println("LOG DE INFRAESTRUTURA: Gateway pagBrasilPay fora do ar. Iniciando Fallback resiliente.");
        String transactionId = UUID.randomUUID().toString();

        // [4] REGRA DO FALLBACK: O saldo FICA INTACTO.
        // Não chamamos o método 'walletService.debit()'.
        // Apenas guardamos o pagamento no banco com status PENDING para rodar em segundo plano depois.
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
