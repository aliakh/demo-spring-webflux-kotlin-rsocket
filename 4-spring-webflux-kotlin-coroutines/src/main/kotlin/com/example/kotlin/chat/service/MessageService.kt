package com.example.kotlin.chat.service

interface MessageService {

    suspend fun latest(): List<MessageViewModel>

    suspend fun after(messageId: String): List<MessageViewModel>

    suspend fun post(message: MessageViewModel)
}
