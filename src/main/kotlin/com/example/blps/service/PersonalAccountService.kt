package com.example.blps.service

import com.example.blps.security.service.XmlUserService
import org.springframework.stereotype.Service

class InsufficientFundsException(msg: String) : RuntimeException(msg)

@Service
class PersonalAccountService(
    private val xmlUserService: XmlUserService
) {
    @Synchronized
    fun getBalance(username: String): Double =
        xmlUserService.findByUsername(username)?.balance
            ?: throw RuntimeException("User $username not found")

    @Synchronized
    fun deposit(username: String, amount: Double) {
        require(amount > 0) { "Amount must be positive" }
        val user = xmlUserService.findByUsername(username)
            ?: throw RuntimeException("User $username not found")
        user.balance += amount
        xmlUserService.saveUser(user)
    }

    @Synchronized
    @Throws(InsufficientFundsException::class)
    fun withdraw(username: String, amount: Double) {
        require(amount > 0) { "Amount must be positive" }
        val user = xmlUserService.findByUsername(username)
            ?: throw RuntimeException("User $username not found")
        if (user.balance < amount)
            throw InsufficientFundsException("Not enough funds on personal account")
        user.balance -= amount
        xmlUserService.saveUser(user)
    }
}