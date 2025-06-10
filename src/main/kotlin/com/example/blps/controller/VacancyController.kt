package com.example.blps.controller

import com.example.blps.dto.VacancyDto
import com.example.blps.model.*
import com.example.blps.service.VacancyService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/vacancies")
class VacancyController(
    private val vacancyService: VacancyService
) {
    @PostMapping
    @PreAuthorize("hasAuthority('VACANCY_CREATE_EDIT')")
    fun createVacancy(@RequestBody vacancy: Vacancy): Vacancy {
        return try {
            vacancyService.createVacancy(vacancy)
        } catch (ex: RuntimeException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to create vacancy", ex)
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VACANCY_VIEW_PUBLIC')")
    fun getVacancy(@PathVariable id: Long): Vacancy {
        return try {
            vacancyService.getVacancyById(id)
        } catch (ex: RuntimeException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, ex.message, ex)
        }
    }

//    @PostMapping("/{id}/prepare-publish")
//    @PreAuthorize("hasAuthority('VACANCY_PUBLISH')")
//    fun prepareForPublishing(
//        @PathVariable id: Long,
//        @RequestBody publishRequest: PublishRequest
//    ): Vacancy {
//        return try {
//            vacancyService.prepareForPublishing(id, publishRequest)
//        } catch (ex: RuntimeException) {
//            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message, ex)
//        }
//    }
//
//    @PostMapping("/{id}/process-payment")
//    @PreAuthorize("hasAuthority('PAYMENT_PROCESS')")
//    fun processPayment(
//        @PathVariable id: Long,
//        @RequestBody paymentRequest: PaymentRequest
//    ): Payment {
//        return try {
//            vacancyService.processPayment(id, paymentRequest.paymentMethod)
//        } catch (ex: RuntimeException) {
//            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message, ex)
//        } catch (ex: IllegalArgumentException) {
//            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message, ex)
//        }
//    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('VACANCY_PUBLISH')")
    fun publish(
        @PathVariable id: Long,
        @RequestBody req: PublishAndPayRequest
    ): Vacancy =
        try {
            vacancyService.publishWithPayment(id, req)
        } catch (ex: RuntimeException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message, ex)
        }

    @GetMapping("/{id}/calculate-cost")
    @PreAuthorize("hasAuthority('VACANCY_VIEW_PUBLIC')")
    fun calculateCost(@PathVariable id: Long): CostResponse {
        return try {
            val vacancy = vacancyService.getVacancyById(id)
            val cost = vacancyService.calculateCost(vacancy)
            CostResponse(cost)
        } catch (ex: RuntimeException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message, ex)
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VACANCY_VIEW_PUBLIC')")
    fun getPublishedVacancies(): List<VacancyDto> {
        return try {
            vacancyService.getPublishedVacancies()
        } catch (ex: RuntimeException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve vacancies", ex)
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('VACANCY_VIEW_ALL')")
    fun getAllVacancies(): List<VacancyDto> {
        return try {
            vacancyService.getAllVacancies()
        } catch (ex: RuntimeException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve all vacancies", ex)
        }
    }
}

//data class PublishRequest(
//    val publishTime: LocalDateTime? = null,
//    val publicationType: PublicationType,
//    val publishOnZarplataRu: Boolean,
//    val cities: List<String>
//)
//
//data class PaymentRequest(
//    val paymentMethod: PaymentMethod
//)

data class PublishAndPayRequest(
    val publishTime: LocalDateTime? = null,
    val publicationType: PublicationType = PublicationType.STANDARD,
    val publishOnZarplataRu: Boolean = false,
    val cities: List<String> = emptyList(),
    val paymentMethod: PaymentMethod
)

data class CostResponse(
    val amount: Double
)