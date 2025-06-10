package com.example.blps.service

import com.example.blps.model.PaymentStatus
import org.springframework.stereotype.Service

@Service
class PaymentGateway(
    private val paymentService: PaymentService
) {
    fun processWithBankCard(paymentId: Long, amount: Double): Boolean {
        // Заглушка для платежного сервиса
        val success = false
        paymentService.updatePaymentStatus(
            paymentId,
            if (success) PaymentStatus.COMPLETED else PaymentStatus.FAILED
        )
        return success
    }

    fun processWithPersonalAccount(paymentId: Long, amount: Double): Boolean {
        // Заглушка для списания с личного счета
        val success = true
        paymentService.updatePaymentStatus(
            paymentId,
            if (success) PaymentStatus.COMPLETED else PaymentStatus.FAILED
        )
        return success
    }

    fun refund(paymentId: Long): Boolean {
        // заглушка возврата
        println("Refund payment $paymentId")
        paymentService.updatePaymentStatus(paymentId, PaymentStatus.REFUNDED)
        return true
    }
}