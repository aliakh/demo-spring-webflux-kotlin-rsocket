package com.example.kotlin.chat.service

import com.example.kotlin.chat.asDomainObject
import com.example.kotlin.chat.asRendered
import com.example.kotlin.chat.mapToViewModel
import com.example.kotlin.chat.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.springframework.stereotype.Service

@Service
class PersistentMessageService(val messageRepository: MessageRepository) : MessageService {

    val sender: MutableSharedFlow<MessageViewModel> = MutableSharedFlow()

    override fun latest(): Flow<MessageViewModel> =
        messageRepository.findLatest()
            .mapToViewModel()

    override fun after(messageId: String): Flow<MessageViewModel> =
        messageRepository.findLatest(messageId)
            .mapToViewModel()

    override fun stream(): Flow<MessageViewModel> = sender

    override suspend fun post(messages: Flow<MessageViewModel>) =
        messages
            .onEach { sender.emit(it.asRendered()) }
            .map { it.asDomainObject() }
            .let { messageRepository.saveAll(it) }
            .collect()
}
