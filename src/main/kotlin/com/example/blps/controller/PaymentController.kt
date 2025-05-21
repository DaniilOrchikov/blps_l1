package com.example.blps.controller

import com.example.blps.model.Payment
import com.example.blps.service.PaymentService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/payments")
class PaymentController(
    private val paymentService: PaymentService
) {
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYMENT_PROCESS')")
    fun getPayment(@PathVariable id: Long): Payment {
        return try {
            paymentService.getPaymentById(id)
        } catch (ex: RuntimeException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, ex.message, ex)
        }
    }
}