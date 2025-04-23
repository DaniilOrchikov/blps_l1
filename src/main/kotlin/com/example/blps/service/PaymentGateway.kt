package com.example.blps.service

import com.example.blps.model.PaymentStatus
import org.springframework.stereotype.Service

@Service
class PaymentGateway(
    private val paymentService: PaymentService
) {
    fun processWithBankCard(paymentId: Long, amount: Double): Boolean {
        // Заглушка для платежного сервиса
        println("Processing payment $paymentId via bank card for amount $amount")
        paymentService.updatePaymentStatus(paymentId, PaymentStatus.COMPLETED)
        return true
    }

    fun processWithPersonalAccount(paymentId: Long, amount: Double): Boolean {
        // Заглушка для списания с личного счета
        println("Processing payment $paymentId via personal account for amount $amount")
        paymentService.updatePaymentStatus(paymentId, PaymentStatus.COMPLETED)
        return true
    }
}