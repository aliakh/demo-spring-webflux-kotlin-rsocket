package com.example.kotlin.chat

import com.example.kotlin.chat.repository.Message
import com.example.kotlin.chat.service.MessageViewModel
import java.time.temporal.ChronoUnit.MILLIS

fun MessageViewModel.prepareForTesting() = copy(id = null, sent = sent.truncatedTo(MILLIS))

fun Message.prepareForTesting() = copy(id = null, sent = sent.truncatedTo(MILLIS))
