package com.example.blps.service

import com.example.blps.model.PaymentMethod
import com.example.blps.model.PaymentStatus
import org.springframework.stereotype.Service

@Service
class PaymentGateway(
    private val paymentService: PaymentService,
    private val personalAccountService: PersonalAccountService
) {
    fun processWithBankCard(paymentId: Long, amount: Double): Boolean {
        // заглушка
        val success = true
        paymentService.updatePaymentStatus(
            paymentId,
            if (success) PaymentStatus.COMPLETED else PaymentStatus.FAILED
        )
        return success
    }

    fun processWithPersonalAccount(
        username: String,
        paymentId: Long,
        amount: Double
    ): Boolean {
        return try {
            personalAccountService.withdraw(username, amount)
            paymentService.updatePaymentStatus(paymentId, PaymentStatus.COMPLETED)
            true
        } catch (ex: InsufficientFundsException) {
            paymentService.updatePaymentStatus(paymentId, PaymentStatus.FAILED)
            false
        }
    }

    fun topUpPersonalAccount(username: String, amount: Double): Boolean {
        // заглушка
        val success = true
        if (success) personalAccountService.deposit(username, amount)
        return success
    }

    fun refund(paymentId: Long): Boolean {
        val payment = paymentService.getPaymentById(paymentId)
        if (payment.status != PaymentStatus.COMPLETED) return false

        val username = payment.username

        if (payment.paymentMethod == PaymentMethod.PERSONAL_ACCOUNT) {
            personalAccountService.deposit(username, payment.amount)
        }
        paymentService.updatePaymentStatus(paymentId, PaymentStatus.REFUNDED)
        return true
    }
}