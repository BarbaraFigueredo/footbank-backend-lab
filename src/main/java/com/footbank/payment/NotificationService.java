package com.footbank.payment;

public interface NotificationService {
    void sendPendingPaymentEmail(String walletId, String transactionId);
}
