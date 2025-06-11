package com.example.blps.service

import com.example.blps.model.Payment
import com.example.blps.model.PaymentMethod
import com.example.blps.model.PaymentStatus
import com.example.blps.repository.PaymentRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

class AccessDeniedForPaymentException(msg: String) : RuntimeException(msg)


@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    transactionManager: PlatformTransactionManager
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRED
        timeout = 30
    }

    fun createPayment(vacancyId: Long, amount: Double, paymentMethod: PaymentMethod, username: String): Payment =
        transactionTemplate.execute { _ ->
            val payment = Payment(
                vacancyId = vacancyId,
                amount = amount,
                paymentMethod = paymentMethod,
                username = username
            )

            paymentRepository.save(payment)
        }!!

    fun getPaymentById(id: Long): Payment {
        return paymentRepository.findById(id).orElseThrow {
            RuntimeException("Payment not found with id: $id")
        }
    }

    fun getLastPaymentForVacancy(vacancyId: Long): Payment? {
        return paymentRepository.findTopByVacancyIdOrderByCreatedAtDesc(vacancyId)
    }

    fun updatePaymentStatus(paymentId: Long, status: PaymentStatus) {
        transactionTemplate.execute { _ ->
            val payment = getPaymentById(paymentId)
            payment.status = status
            payment.processedAt = LocalDateTime.now()
            paymentRepository.save(payment)
        }
    }

    fun getPaymentOfUser(paymentId: Long): Payment =
        paymentRepository.findByIdAndUsername(paymentId, SecurityContextHolder.getContext().authentication.name)
            ?: throw AccessDeniedForPaymentException(
                "Payment $paymentId not found or not yours"
            )
}