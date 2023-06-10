package com.example.kotlin.chat.service

import java.net.URL
import java.time.Instant

data class MessageViewModel(
    val content: String,
    val user: UserViewModel,
    val sent: Instant,
    val id: String? = null
)

data class UserViewModel(
    val name: String,
    val avatarImageLink: URL
)
