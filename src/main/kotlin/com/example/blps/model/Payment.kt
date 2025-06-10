package com.example.blps.model


import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
data class Payment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var vacancyId: Long,
    var amount: Double,
    var paymentMethod: PaymentMethod,

    var username: String,

    var status: PaymentStatus = PaymentStatus.PENDING,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var processedAt: LocalDateTime? = null
)

enum class PaymentMethod {
    BANK_CARD, PERSONAL_ACCOUNT
}

enum class PaymentStatus {
    PENDING, COMPLETED, FAILED, REFUNDED
}