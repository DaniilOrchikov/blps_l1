package com.example.blps.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
data class Vacancy(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var title: String = "",
    var code: String? = null,
    var specialization: String = "",
    var hirePlan: Int? = null,
    var startDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    var employmentType: EmploymentType = EmploymentType.FULL_TIME,

    var workSchedule: String = "",
    var dailyHours: Int = 8,

    @Enumerated(EnumType.STRING)
    var workFormat: WorkFormat = WorkFormat.OFFICE,

    var salaryFrom: Int? = null,
    var salaryTo: Int? = null,

    @Enumerated(EnumType.STRING)
    var paymentFrequency: PaymentFrequency = PaymentFrequency.MONTHLY,

    var workAddress: String? = null,

    @Enumerated(EnumType.STRING)
    var experience: Experience = Experience.NO_EXPERIENCE,

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    @ElementCollection
    var skills: List<String> = mutableListOf(),

    var contactName: String = "",
    var contactEmail: String = "",
    var contactPhone: String = "",

    var publishTime: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    var publicationType: PublicationType = PublicationType.STANDARD,

    var publishOnZarplataRu: Boolean = false,

    @ElementCollection
    var cities: List<String> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    var status: VacancyStatus = VacancyStatus.DRAFT,

    var createdAt: LocalDateTime = LocalDateTime.now(),
    var publishedAt: LocalDateTime? = null,
    var expiresAt: LocalDateTime? = null,

    var promotionScore: Double = 0.0,
    var lastPromotionUpdate: LocalDateTime? = null
)

enum class EmploymentType {
    FULL_TIME, PART_TIME, CONTRACT, TEMPORARY, INTERNSHIP
}

enum class WorkFormat {
    OFFICE, REMOTE, HYBRID
}

enum class PaymentFrequency {
    WEEKLY, BIWEEKLY, MONTHLY, DAILY
}

enum class Experience {
    NO_EXPERIENCE, ONE_TO_THREE_YEARS, THREE_TO_SIX_YEARS, MORE_THAN_SIX_YEARS
}

enum class PublicationType {
    STANDARD, STANDARD_PLUS, PREMIUM
}

enum class VacancyStatus {
    DRAFT, PENDING_PAYMENT, PUBLISHED, EXPIRED
}