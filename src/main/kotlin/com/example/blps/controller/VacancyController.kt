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

    @PostMapping("/{id}/prepare-publish")
    fun prepareForPublishing(
        @PathVariable id: Long,
        @RequestBody publishRequest: PublishRequest
    ): Vacancy {
        return try {
            vacancyService.prepareForPublishing(id, publishRequest)
        } catch (ex: RuntimeException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message, ex)
        }
    }

    @PostMapping("/{id}/process-payment")
    fun processPayment(
        @PathVariable id: Long,
        @RequestBody paymentRequest: PaymentRequest
    ): Payment {
        return try {
            vacancyService.processPayment(id, paymentRequest.paymentMethod)
        } catch (ex: RuntimeException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message, ex)
        } catch (ex: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message, ex)
        }
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
    fun getAllVacancies(): List<VacancyDto> {
        return try {
            vacancyService.getAllVacancies()
        } catch (ex: RuntimeException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve vacancies", ex)
        }
    }
}

data class PublishRequest(
    val publishTime: LocalDateTime? = null,
    val publicationType: PublicationType,
    val publishOnZarplataRu: Boolean,
    val cities: List<String>
)

data class PaymentRequest(
    val paymentMethod: PaymentMethod
)

data class CostResponse(
    val amount: Double
)