package com.example.blps.service

import com.example.blps.controller.PublishRequest
import com.example.blps.dto.VacancyDto
import com.example.blps.model.*
import com.example.blps.repository.VacancyRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class VacancyService(
    private val vacancyRepository: VacancyRepository,
    private val paymentService: PaymentService,
    private val zarplataRuService: ZarplataRuService,
    private val paymentGateway: PaymentGateway
) {
    companion object {
        private const val BASE_PROMOTION_SCORE = 100.0
        private const val BONUS = 100.0
        private const val DAILY_SCORE_DECREASE = 5.0
    }

    fun createVacancy(vacancy: Vacancy): Vacancy {
        return vacancyRepository.save(vacancy.apply {
            status = VacancyStatus.DRAFT
            createdAt = LocalDateTime.now()
        })
    }

    fun getVacancyById(id: Long): Vacancy {
        return vacancyRepository.findById(id).orElseThrow {
            RuntimeException("Vacancy not found with id: $id")
        }
    }

    fun prepareForPublishing(vacancyId: Long, publishRequest: PublishRequest): Vacancy {
        val vacancy = getVacancyById(vacancyId).apply {
            publishTime = publishRequest.publishTime
            publicationType = publishRequest.publicationType
            publishOnZarplataRu = publishRequest.publishOnZarplataRu
            cities = publishRequest.cities
            status = VacancyStatus.PENDING_PAYMENT
        }
        return vacancyRepository.save(vacancy)
    }

    fun processPayment(vacancyId: Long, paymentMethod: PaymentMethod): Payment {
        val vacancy = getVacancyById(vacancyId)
        require(vacancy.status == VacancyStatus.PENDING_PAYMENT) {
            "Vacancy is not in PENDING_PAYMENT status"
        }

        val amount = calculateCost(vacancy)
        val payment = paymentService.createPayment(vacancyId, amount, paymentMethod)

        if (paymentMethod == PaymentMethod.PERSONAL_ACCOUNT) {
            paymentGateway.processWithPersonalAccount(payment.id, amount)
        } else {
            paymentGateway.processWithBankCard(payment.id, amount)
        }

        // Автоматическая публикация, если не указано отложенное время
        if (vacancy.publishTime == null || vacancy.publishTime!!.isBefore(LocalDateTime.now())) {
            publishVacancy(vacancy)
        }

        return payment
    }

    fun calculateCost(vacancy: Vacancy): Double {
        var basePrice = when (vacancy.publicationType) {
            PublicationType.STANDARD -> 1358.0
            PublicationType.STANDARD_PLUS -> 4277.0
            PublicationType.PREMIUM -> 12542.0
        }

        if (vacancy.publishOnZarplataRu) {
            basePrice += 2100
        }

        return basePrice * vacancy.cities.size
    }

    @Scheduled(fixedRate = 60000) // Проверка каждую минуту
    fun publishScheduledVacancies() {
        val now = LocalDateTime.now()
        vacancyRepository.findByStatus(VacancyStatus.PENDING_PAYMENT)
            .filter { it.publishTime == null || it.publishTime!!.isBefore(now) }
            .forEach { vacancy ->
                paymentService.getLastPaymentForVacancy(vacancy.id)?.takeIf { it.status == PaymentStatus.COMPLETED }
                    ?.let { publishVacancy(vacancy) }
            }
    }

    @Scheduled(fixedRate = 3600000) // Проверка каждый час
    fun expireOldVacancies() {
        val now = LocalDateTime.now()
        vacancyRepository.findByStatus(VacancyStatus.PUBLISHED)
            .filter { it.expiresAt != null && it.expiresAt!!.isBefore(now) }
            .forEach { vacancy ->
                vacancy.status = VacancyStatus.EXPIRED
                vacancyRepository.save(vacancy)
            }
    }

    fun publishVacancy(vacancy: Vacancy): Vacancy {
        require(vacancy.status == VacancyStatus.PENDING_PAYMENT) {
            "Cannot publish vacancy with status ${vacancy.status}"
        }

        val actualPublishTime = vacancy.publishTime ?: LocalDateTime.now()

        return vacancy.apply {
            publishedAt = actualPublishTime
            expiresAt = calculateExpirationDate(actualPublishTime)
            status = VacancyStatus.PUBLISHED
            promotionScore = calculateInitialPromotionScore(vacancy)
            lastPromotionUpdate = actualPublishTime

            if (publishOnZarplataRu) {
                zarplataRuService.publishVacancy(this)
            }
        }.let { vacancyRepository.save(it) }
    }
    private fun calculateInitialPromotionScore(vacancy: Vacancy): Double {
        return when (vacancy.publicationType) {
            PublicationType.STANDARD -> BASE_PROMOTION_SCORE
            PublicationType.STANDARD_PLUS -> BASE_PROMOTION_SCORE + BONUS
            PublicationType.PREMIUM -> BASE_PROMOTION_SCORE + BONUS
        }
    }

    private fun calculateExpirationDate(publishTime: LocalDateTime): LocalDateTime {
        return publishTime.plusDays(30)
    }

    @Scheduled(cron = "0 0 * * * ?")
    fun updatePromotionScores() {
        val now = LocalDateTime.now()
        vacancyRepository.findByStatus(VacancyStatus.PUBLISHED).forEach { vacancy ->
            val daysSincePublish = ChronoUnit.DAYS.between(vacancy.publishedAt, now)

            when (vacancy.publicationType) {
                PublicationType.STANDARD -> {
                    // Для STANDARD - уменьшаем рейтинг со временем
                    vacancy.promotionScore = (BASE_PROMOTION_SCORE - (DAILY_SCORE_DECREASE * daysSincePublish))
                        .coerceAtLeast(0.0)
                }
                PublicationType.STANDARD_PLUS -> {
                    // Для STANDARD_PLUS обновляем каждые 3 дня
                    if (daysSincePublish % 3 == 0L &&
                        (vacancy.lastPromotionUpdate == null ||
                                ChronoUnit.DAYS.between(vacancy.lastPromotionUpdate, now) >= 3)) {
                        vacancy.promotionScore = (BASE_PROMOTION_SCORE + BONUS -
                                (DAILY_SCORE_DECREASE * (daysSincePublish / 3)))
                            .coerceAtLeast(0.0)
                        vacancy.lastPromotionUpdate = now
                    }
                }
                PublicationType.PREMIUM -> {
                    // Для PREMIUM первые 7 дней без уменьшения
                    if (daysSincePublish > 7) {
                        vacancy.promotionScore = (BASE_PROMOTION_SCORE + BONUS -
                                (DAILY_SCORE_DECREASE * (daysSincePublish - 7)))
                            .coerceAtLeast(0.0)
                    }
                }
            }
            vacancyRepository.save(vacancy)
        }
    }

    @Scheduled(cron = "0 0 3 * * ?") // Ежедневно
    fun promoteStandardPlusVacancies() {
        val now = LocalDateTime.now()
        vacancyRepository.findByPublicationTypeAndStatus(
            PublicationType.STANDARD_PLUS,
            VacancyStatus.PUBLISHED
        ).filter {
            it.publishedAt!!.isBefore(now.minusDays(3)) &&
                    (it.lastPromotionUpdate == null ||
                            it.lastPromotionUpdate!!.isBefore(now.minusDays(3)))
        }.forEach { vacancy ->
            vacancy.promotionScore = BASE_PROMOTION_SCORE + BONUS
            vacancy.lastPromotionUpdate = now
            vacancyRepository.save(vacancy)
        }
    }

    fun getAllVacancies(): List<VacancyDto> {
        return vacancyRepository.findAllByOrderByPromotionScoreDesc()
            .map { it.toDto() }
    }
}


private fun Vacancy.toDto() = VacancyDto(
    id = id,
    title = title,
    code = code,
    specialization = specialization,
    hirePlan = hirePlan,
    startDate = startDate,
    employmentType = employmentType,
    workSchedule = workSchedule,
    dailyHours = dailyHours,
    workFormat = workFormat,
    salaryFrom = salaryFrom,
    salaryTo = salaryTo,
    paymentFrequency = paymentFrequency,
    workAddress = workAddress,
    experience = experience,
    description = description,
    skills = skills,
    contactName = contactName,
    contactEmail = contactEmail,
    contactPhone = contactPhone,
    publishTime = publishTime,
    publicationType = publicationType,
    publishOnZarplataRu = publishOnZarplataRu,
    cities = cities,
    status = status,
    createdAt = createdAt,
    publishedAt = publishedAt,
    expiresAt = expiresAt,
    promotionScore = promotionScore,
    lastPromotionUpdate = lastPromotionUpdate
)