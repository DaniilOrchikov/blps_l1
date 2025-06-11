package com.example.blps.controller

import com.example.blps.dto.VacancyDto
import com.example.blps.model.*
import com.example.blps.service.VacancyService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/vacancies")
class VacancyController(
    private val vacancyService: VacancyService
) {
    @PostMapping
    fun createVacancy(@RequestBody vacancy: Vacancy): Vacancy {
        return try {
            vacancyService.createVacancy(vacancy)
        } catch (ex: RuntimeException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to create vacancy", ex)
        }
    }

    @GetMapping("/{id}")
    fun getVacancy(@PathVariable id: Long): Vacancy {
        return try {
            vacancyService.getVacancyById(id)
        } catch (ex: RuntimeException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, ex.message, ex)
        }
    }

    @PostMapping("/{id}/publish")
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
    fun getPublishedVacancies(): List<VacancyDto> {
        return try {
            vacancyService.getPublishedVacancies()
        } catch (ex: RuntimeException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve vacancies", ex)
        }
    }

    @GetMapping("/all")
    fun getAllVacancies(): List<VacancyDto> {
        return try {
            vacancyService.getAllVacancies()
        } catch (ex: RuntimeException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve all vacancies", ex)
        }
    }
}

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