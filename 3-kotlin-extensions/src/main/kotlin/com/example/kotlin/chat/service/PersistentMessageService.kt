package com.example.kotlin.chat.service

import com.example.kotlin.chat.asDomainObject
import com.example.kotlin.chat.mapToViewModel
import com.example.kotlin.chat.repository.MessageRepository
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class PersistentMessageService(val messageRepository: MessageRepository) : MessageService {

    override fun latest(): List<MessageViewModel> =
        messageRepository.findLatest()
            .mapToViewModel()

    override fun after(messageId: String): List<MessageViewModel> =
        messageRepository.findLatest(messageId)
            .mapToViewModel()

    override fun post(message: MessageViewModel) {
        messageRepository.save(message.asDomainObject())
    }
}
