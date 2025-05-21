package com.example.blps.security.model

import jakarta.xml.bind.annotation.*

@XmlRootElement(name = "users")
@XmlAccessorType(XmlAccessType.FIELD)
data class UsersList(
    @XmlElement(name = "user")
    val users: MutableList<User> = mutableListOf()
)

@XmlAccessorType(XmlAccessType.FIELD)
data class User(
    @XmlAttribute
    val username: String = "",

    @XmlElement
    val password: String = "",

    @XmlElement
    val email: String = "",

    @XmlElement(name = "role")
    val roles: MutableList<String> = mutableListOf()
)