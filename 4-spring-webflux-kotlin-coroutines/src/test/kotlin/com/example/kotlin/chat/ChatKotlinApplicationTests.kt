package com.example.kotlin.chat

import com.example.kotlin.chat.repository.ContentType
import com.example.kotlin.chat.repository.Message
import com.example.kotlin.chat.repository.MessageRepository
import com.example.kotlin.chat.service.MessageViewModel
import com.example.kotlin.chat.service.UserViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import java.net.URI
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit.MILLIS

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.r2dbc.url=r2dbc:h2:mem:///testdb;USER=sa;PASSWORD=password"
    ]
)
class ChatKotlinApplicationTests {

    @Autowired
    lateinit var client: TestRestTemplate

    @Autowired
    lateinit var messageRepository: MessageRepository

    lateinit var lastMessageId: String

    val now: Instant = Instant.now()

    @BeforeEach
    fun setUp() {
        runBlocking {
            val secondBeforeNow = now.minusSeconds(1)
            val twoSecondBeforeNow = now.minusSeconds(2)
            val savedMessages = messageRepository.saveAll(
                listOf(
                    Message(
                        "*message1*",
                        ContentType.PLAIN,
                        twoSecondBeforeNow,
                        "test",
                        "http://test.com"
                    ),
                    Message(
                        "**message2**",
                        ContentType.MARKDOWN,
                        secondBeforeNow,
                        "user1",
                        "http://user1.com"
                    ),
                    Message(
                        "`message3`",
                        ContentType.MARKDOWN,
                        now,
                        "user2",
                        "http://user2.com"
                    )
                )
            )
            lastMessageId = savedMessages.first().id ?: ""
        }
    }

    @AfterEach
    fun tearDown() {
        runBlocking {
            messageRepository.deleteAll()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that messages API returns latest messages`(withLastMessageId: Boolean) {
        runBlocking {

            val messages: List<MessageViewModel>? = client.exchange(
                RequestEntity<Any>(
                    HttpMethod.GET,
                    URI("/api/v1/messages?lastMessageId=${if (withLastMessageId) lastMessageId else ""}")
                ),
                object : ParameterizedTypeReference<List<MessageViewModel>>() {}).body

            if (!withLastMessageId) {
                assertThat(messages?.map { with(it) { copy(id = null, sent = sent.truncatedTo(MILLIS)) } })
                    .first()
                    .isEqualTo(
                        MessageViewModel(
                            "*message1*",
                            UserViewModel("test", URL("http://test.com")),
                            now.minusSeconds(2).truncatedTo(MILLIS)
                        )
                    )
            }

            assertThat(messages?.map { it.prepareForTesting() })
                .containsSubsequence(
                    MessageViewModel(
                        "<body><p><strong>message2</strong></p></body>",
                        UserViewModel("user1", URL("http://user1.com")),
                        now.minusSeconds(1).truncatedTo(MILLIS)
                    ),
                    MessageViewModel(
                        "<body><p><code>message3</code></p></body>",
                        UserViewModel("user2", URL("http://user2.com")),
                        now.truncatedTo(MILLIS)
                    )
                )
        }
    }

    @Test
    fun `test that messages posted to the API is stored`() {
        runBlocking {
            client.postForEntity<Any>(
                URI("/api/v1/messages"),
                MessageViewModel(
                    "`message4`",
                    UserViewModel("test", URL("http://test.com")),
                    now.plusSeconds(1)
                )
            )
            messageRepository.findAll()
                .first { it.content.contains("message4") }
                .apply {
                    assertThat(this.prepareForTesting())
                        .isEqualTo(
                            Message(
                                "`message4`",
                                ContentType.MARKDOWN,
                                now.plusSeconds(1).truncatedTo(MILLIS),
                                "test",
                                "http://test.com"
                            )
                        )
                }
        }
    }
}
