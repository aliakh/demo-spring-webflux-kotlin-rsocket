package com.example.kotlin.chat.service

import com.github.javafaker.Faker
import org.springframework.stereotype.Service
import java.net.URL
import java.time.Instant
import kotlin.random.Random

@Service
class FakeMessageService : MessageService {

    val faker: Faker = Faker.instance()

    val users: Map<String, UserViewModel> = mapOf(
        "Darth Vader" to UserViewModel("Darth Vader", URL("http://localhost:8080/images/Darth_Vader.png")),
        "Obi-Wan Kenobi" to UserViewModel("Obi-Wan Kenobi", URL("http://localhost:8080/images/Obi_Wan_Kenobi.png")),
        "Yoda" to UserViewModel("Yoda", URL("http://localhost:8080/images/Yoda.png"))
    )

    val usersQuotes: Map<String, () -> String> = mapOf(
        "Darth Vader" to { faker.resolve("star_wars.quotes.darth_vader") },
        "Obi-Wan Kenobi" to { faker.resolve("star_wars.quotes.obi_wan_kenobi") },
        "Yoda" to { faker.resolve("star_wars.quotes.yoda") },
    )

    override fun latest(): List<MessageViewModel> {
        val count = Random.nextInt(1, 3)
        return (0..count).map {
            val user = users.values.random()
            val userQuote = usersQuotes.getValue(user.name).invoke()

            MessageViewModel(userQuote, user, Instant.now(), Random.nextBytes(10).toString())
        }.toList()
    }

    override fun after(messageId: String): List<MessageViewModel> {
        return latest()
    }

    override fun post(message: MessageViewModel) {
        TODO("Not yet implemented")
    }
}
