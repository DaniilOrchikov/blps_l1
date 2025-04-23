package com.example.blps.repository

import com.example.blps.model.PublicationType
import com.example.blps.model.Vacancy
import com.example.blps.model.VacancyStatus
import org.springframework.data.jpa.repository.JpaRepository

interface VacancyRepository : JpaRepository<Vacancy, Long> {
    fun findAllByOrderByPromotionScoreDesc(): List<Vacancy>
    fun findByStatus(status: VacancyStatus): List<Vacancy>
    fun findByPublicationTypeAndStatus(type: PublicationType, status: VacancyStatus): List<Vacancy>
}