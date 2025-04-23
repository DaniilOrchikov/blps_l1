package com.example.blps.repository

import com.example.blps.model.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findTopByVacancyIdOrderByCreatedAtDesc(vacancyId: Long): Payment?
}