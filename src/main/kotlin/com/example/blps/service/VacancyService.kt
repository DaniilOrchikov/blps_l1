package com.example.blps.service

import com.example.blps.controller.PublishAndPayRequest
import com.example.blps.dto.VacancyDto
import com.example.blps.model.*
import com.example.blps.repository.VacancyRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class VacancyService(
    private val vacancyRepository: VacancyRepository,
    private val paymentService: PaymentService,
    private val zarplataRuService: ZarplataRuService,
    private val paymentGateway: PaymentGateway,
    transactionManager: PlatformTransactionManager
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRED
        timeout = 30
    }

    private val readOnlyTransactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRED
        isReadOnly = true
    }

    private val serializableTransactionTemplate = TransactionTemplate(transactionManager).apply {
        isolationLevel = TransactionDefinition.ISOLATION_SERIALIZABLE
    }

    private val batchTransactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        timeout = 120
    }

    companion object {
        private const val BASE_PROMOTION_SCORE = 100.0
        private const val BONUS = 100.0
        private const val DAILY_SCORE_DECREASE = 5.0
    }

    fun createVacancy(vacancy: Vacancy): Vacancy =
        transactionTemplate.execute { _ ->
            vacancyRepository.save(vacancy.apply {
                status = VacancyStatus.DRAFT
                createdAt = LocalDateTime.now()
            })
        }!!

    fun getVacancyById(id: Long): Vacancy =
        readOnlyTransactionTemplate.execute { _ ->
            vacancyRepository.findById(id).orElseThrow {
                RuntimeException("Vacancy not found with id: $id")
            }
        }!!

//    fun prepareForPublishing(vacancyId: Long, publishRequest: PublishRequest): Vacancy =
//        transactionTemplate.execute { _ ->
//            val vacancy = getVacancyById(vacancyId).apply {
//                publishTime = publishRequest.publishTime
//                publicationType = publishRequest.publicationType
//                publishOnZarplataRu = publishRequest.publishOnZarplataRu
//                cities = publishRequest.cities
//                status = VacancyStatus.PENDING_PAYMENT
//            }
//            vacancyRepository.save(vacancy)
//        }!!
//
//    fun processPayment(vacancyId: Long, paymentMethod: PaymentMethod): Payment =
//        serializableTransactionTemplate.execute { _ ->
//            val vacancy = getVacancyById(vacancyId)
//            require(vacancy.status == VacancyStatus.PENDING_PAYMENT) {
//                "Vacancy is not in PENDING_PAYMENT status"
//            }
//
//            val amount = calculateCost(vacancy)
//            val payment = paymentService.createPayment(vacancyId, amount, paymentMethod)
//
//            if (paymentMethod == PaymentMethod.PERSONAL_ACCOUNT) {
//                paymentGateway.processWithPersonalAccount(payment.id, amount)
//            } else {
//                paymentGateway.processWithBankCard(payment.id, amount)
//            }
//
//            // Автоматическая публикация, если не указано отложенное время
//            if (vacancy.publishTime == null || vacancy.publishTime!!.isBefore(LocalDateTime.now())) {
//                publishVacancy(vacancy)
//            }
//
//            payment
//        }!!

    fun publishWithPayment(vacancyId: Long, req: PublishAndPayRequest): Vacancy {
        val paidVacancy = transactionTemplate.execute { _ ->

            val vacancy = getVacancyById(vacancyId)
            require(vacancy.status == VacancyStatus.DRAFT) {
                "Vacancy must be in DRAFT status"
            }

            vacancy.apply {
                publishTime = req.publishTime
                publicationType = req.publicationType
                publishOnZarplataRu = req.publishOnZarplataRu
                cities = req.cities.toMutableList()
                status = VacancyStatus.PENDING_PAYMENT
            }

            val price = calculateCost(vacancy)
            val payment = paymentService.createPayment(
                vacancyId, price, req.paymentMethod
            )

            val payOk = when (req.paymentMethod) {
                PaymentMethod.BANK_CARD -> paymentGateway.processWithBankCard(payment.id, price)
                PaymentMethod.PERSONAL_ACCOUNT -> paymentGateway.processWithPersonalAccount(payment.id, price)
            }

            if (!payOk)
                throw RuntimeException("Payment failed")

            vacancy.status = VacancyStatus.PAID
            vacancyRepository.save(vacancy)
        }!!

        return try {
            if (paidVacancy.publishTime == null ||
                paidVacancy.publishTime!!.isBefore(LocalDateTime.now())
            )
                publishPaidVacancy(paidVacancy)
            else
                paidVacancy
        } catch (ex: Exception) {
            rollbackAfterPublishFail(paidVacancy.id)
            throw ex
        }
    }

    private fun publishPaidVacancy(vacancy: Vacancy): Vacancy =
        transactionTemplate.execute { _ ->

            require(vacancy.status == VacancyStatus.PAID) {
                "Vacancy must be PAID before publishing"
            }

            val publishMoment = vacancy.publishTime ?: LocalDateTime.now()

            vacancy.apply {
                publishedAt = publishMoment
                expiresAt = calculateExpirationDate(publishMoment)
                status = VacancyStatus.PUBLISHED
                promotionScore = calculateInitialPromotionScore(this)
                lastPromotionUpdate = publishMoment
            }

            if (vacancy.publishOnZarplataRu) {
                val ok = zarplataRuService.publishVacancy(vacancy)
                if (!ok) throw RuntimeException("Zarplata.ru returned false")
            }

            vacancyRepository.save(vacancy)
        }!!

    private fun rollbackAfterPublishFail(vacancyId: Long) {
        transactionTemplate.execute { _ ->
            val vacancy = getVacancyById(vacancyId)
            vacancy.apply {
                status = VacancyStatus.DRAFT
                publishTime = null
                publishOnZarplataRu = false
                cities.clear()
            }
            vacancyRepository.save(vacancy)

            paymentService.getLastPaymentForVacancy(vacancyId)?.let {
                if (it.status == PaymentStatus.COMPLETED)
                    paymentGateway.refund(it.id)
            }
        }
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

    @Scheduled(fixedRate = 60_000)
    fun publishScheduledVacancies() {
        transactionTemplate.execute { _ ->
            val now = LocalDateTime.now()
            vacancyRepository.findByStatus(VacancyStatus.PAID)
                .filter { it.publishTime != null && it.publishTime!!.isBefore(now) }
                .forEach {
                    try {
                        publishPaidVacancy(it)
                    } catch (ex: Exception) {
                        rollbackAfterPublishFail(it.id)
                    }
                }
        }
    }


    @Scheduled(fixedRate = 3600000)
    fun expireOldVacancies() {
        transactionTemplate.execute { _ ->
            val now = LocalDateTime.now()
            vacancyRepository.findByStatus(VacancyStatus.PUBLISHED)
                .filter { it.expiresAt != null && it.expiresAt!!.isBefore(now) }
                .forEach { vac ->
                    vac.status = VacancyStatus.EXPIRED
                    vacancyRepository.save(vac)
                }
        }
    }

//    fun publishVacancy(vacancy: Vacancy): Vacancy =
//        transactionTemplate.execute { _ ->
//            require(vacancy.status == VacancyStatus.PENDING_PAYMENT) {
//                "Cannot publish vacancy with status ${vacancy.status}"
//            }
//
//            val actualPublishTime = vacancy.publishTime ?: LocalDateTime.now()
//
//            vacancy.apply {
//                publishedAt = actualPublishTime
//                expiresAt = calculateExpirationDate(actualPublishTime)
//                status = VacancyStatus.PUBLISHED
//                promotionScore = calculateInitialPromotionScore(vacancy)
//                lastPromotionUpdate = actualPublishTime
//
//                if (publishOnZarplataRu) {
//                    zarplataRuService.publishVacancy(this)
//                }
//            }.let { vacancyRepository.save(it) }
//        }!!


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
        batchTransactionTemplate.execute { _ ->
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
                                    ChronoUnit.DAYS.between(vacancy.lastPromotionUpdate, now) >= 3)
                        ) {
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
    }

    @Scheduled(cron = "0 0 3 * * ?") // Ежедневно
    fun promoteStandardPlusVacancies() {
        transactionTemplate.execute { _ ->
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
    }

    fun getAllVacancies(): List<VacancyDto> =
        readOnlyTransactionTemplate.execute { _ ->
            vacancyRepository.findAllByOrderByPromotionScoreDesc()
                .map { it.toDto() }
        }!!

    fun getPublishedVacancies(): List<VacancyDto> =
        readOnlyTransactionTemplate.execute { _ ->
            vacancyRepository.findByStatus(VacancyStatus.PUBLISHED)
                .map { it.toDto() }
        }!!
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