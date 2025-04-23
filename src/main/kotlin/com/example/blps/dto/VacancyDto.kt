package com.example.blps.dto

import com.example.blps.model.*
import java.time.LocalDate
import java.time.LocalDateTime

data class VacancyDto(
    val id: Long,
    val title: String,
    val code: String?,
    val specialization: String,
    val hirePlan: Int?,
    val startDate: LocalDate?,
    val employmentType: EmploymentType,
    val workSchedule: String,
    val dailyHours: Int,
    val workFormat: WorkFormat,
    val salaryFrom: Int?,
    val salaryTo: Int?,
    val paymentFrequency: PaymentFrequency,
    val workAddress: String?,
    val experience: Experience,
    val description: String,
    val skills: List<String>,
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String,
    val publishTime: LocalDateTime?,
    val publicationType: PublicationType,
    val publishOnZarplataRu: Boolean,
    val cities: List<String>,
    val status: VacancyStatus,
    val createdAt: LocalDateTime,
    val publishedAt: LocalDateTime?,
    val expiresAt: LocalDateTime?,
    val promotionScore:Double,
    val lastPromotionUpdate:LocalDateTime?
)