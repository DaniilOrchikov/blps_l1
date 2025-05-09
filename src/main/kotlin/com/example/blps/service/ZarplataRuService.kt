package com.example.blps.service

import com.example.blps.model.Vacancy
import org.springframework.stereotype.Service

@Service
class ZarplataRuService {
    fun publishVacancy(vacancy: Vacancy): Boolean {
        // Заглушка для интеграции с Зарплата.ру
        println("Publishing vacancy ${vacancy.id} to Zarplata.ru")
        return true
    }
}