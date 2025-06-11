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
            vacancyService.getOwnVacancy(id)
        } catch (ex: RuntimeException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, ex.message, ex)
        }
    }

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

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('VACANCY_VIEW_MINE')")
    fun getMineVacancies(): List<VacancyDto> {
        return try {
            vacancyService.getAllMyVacancies()
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