package com.example.kotlin.chat.service

import kotlinx.coroutines.flow.Flow

interface MessageService {

    fun latest(): Flow<MessageViewModel>

    fun after(messageId: String): Flow<MessageViewModel>

    fun stream(): Flow<MessageViewModel>

    suspend fun post(messages: Flow<MessageViewModel>)
}
