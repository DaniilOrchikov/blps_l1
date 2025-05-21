package com.example.blps.security.service

import com.example.blps.security.model.User
import com.example.blps.security.model.UsersList
import jakarta.annotation.PostConstruct
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import org.springframework.stereotype.Service
import java.io.File

@Service
class XmlUserService {
    private val xmlFilePath = "users.xml"
    private val jaxbContext = JAXBContext.newInstance(UsersList::class.java)

    @PostConstruct
    fun init() {
        val file = File(xmlFilePath)
        if (!file.exists()) {
            createEmptyXmlFile()
        }
    }

    private fun createEmptyXmlFile() {
        val emptyUsersList = UsersList().apply {
            users
        }

        saveUsers(emptyUsersList)
    }

    fun getUsers(): UsersList {
        val file = File(xmlFilePath)
        val unmarshaller = jaxbContext.createUnmarshaller()
        return unmarshaller.unmarshal(file) as UsersList
    }

    fun findByUsername(username: String): User? {
        return getUsers().users.find { it.username == username }
    }

    fun saveUser(user: User) {
        val users = getUsers()
        // Удаляем пользователя если он уже существует
        users.users.removeIf { it.username == user.username }
        users.users.add(user)
        saveUsers(users)
    }

    private fun saveUsers(users: UsersList) {
        val file = File(xmlFilePath)
        val marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        marshaller.marshal(users, file)
    }

    fun existsByUsername(username: String): Boolean {
        return getUsers().users.any { it.username == username }
    }

    fun existsByEmail(email: String): Boolean {
        return getUsers().users.any { it.email == email }
    }
}