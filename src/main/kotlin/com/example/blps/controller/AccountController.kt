package com.example.blps.controller

import com.example.blps.service.PaymentGateway
import com.example.blps.service.PersonalAccountService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.security.core.context.SecurityContextHolder

@RestController
@RequestMapping("/api/account")
class AccountController(
    private val accountService: PersonalAccountService,
    private val paymentGateway: PaymentGateway
) {
    data class TopUpRequest(val amount: Double)

    @GetMapping("/balance")
    fun balance(): Map<String, Double> {
        val username = SecurityContextHolder.getContext().authentication.name
        return mapOf("balance" to accountService.getBalance(username))
    }

    @PostMapping("/topup")
    fun topUp(@RequestBody req: TopUpRequest): Map<String, Double> {
        val username = SecurityContextHolder.getContext().authentication.name
        val ok = paymentGateway.topUpPersonalAccount(username, req.amount)
        if (!ok) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Top-up failed")
        return mapOf("balance" to accountService.getBalance(username))
    }
}