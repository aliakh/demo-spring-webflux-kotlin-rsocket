package com.example.kotlin.chat

import app.cash.turbine.test
import com.example.kotlin.chat.repository.ContentType
import com.example.kotlin.chat.repository.Message
import com.example.kotlin.chat.repository.MessageRepository
import com.example.kotlin.chat.service.MessageViewModel
import com.example.kotlin.chat.service.UserViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.dataWithType
import org.springframework.messaging.rsocket.retrieveFlow
import java.net.URI
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit.MILLIS
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.r2dbc.url=r2dbc:h2:mem:///testdb;USER=sa;PASSWORD=password"
    ]
)
class ChatKotlinApplicationTests(
    @Autowired val rsocketBuilder: RSocketRequester.Builder,
    @Autowired val messageRepository: MessageRepository,
    @LocalServerPort val serverPort: Int
) {

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
                        "*message*",
                        ContentType.PLAIN,
                        twoSecondBeforeNow,
                        "user",
                        "http://user1.com"
                    ),
                    Message(
                        "**message2**",
                        ContentType.MARKDOWN,
                        secondBeforeNow,
                        "user2",
                        "http://user2.com"
                    ),
                    Message(
                        "`message3`",
                        ContentType.MARKDOWN,
                        now,
                        "user3",
                        "http://user3.com"
                    )
                )
            ).toList()
            lastMessageId = savedMessages.first().id ?: ""
        }
    }

    @AfterEach
    fun tearDown() {
        runBlocking {
            messageRepository.deleteAll()
        }
    }

    @ExperimentalTime
    @ExperimentalCoroutinesApi
    @Test
    fun `test that messages API streams latest messages`() {
        runBlocking {
            val rSocketRequester = rsocketBuilder.websocket(URI("ws://localhost:${serverPort}/rsocket"))

            rSocketRequester
                .route("api.v1.messages.stream")
                .retrieveFlow<MessageViewModel>()
                .test {
                    assertThat(expectItem().prepareForTesting())
                        .isEqualTo(
                            MessageViewModel(
                                "*message*",
                                UserViewModel("user", URL("http://user1.com")),
                                now.minusSeconds(2).truncatedTo(MILLIS)
                            )
                        )

                    assertThat(expectItem().prepareForTesting())
                        .isEqualTo(
                            MessageViewModel(
                                "<body><p><strong>message2</strong></p></body>",
                                UserViewModel("user2", URL("http://user2.com")),
                                now.minusSeconds(1).truncatedTo(MILLIS)
                            )
                        )
                    assertThat(expectItem().prepareForTesting())
                        .isEqualTo(
                            MessageViewModel(
                                "<body><p><code>message3</code></p></body>",
                                UserViewModel("user3", URL("http://user3.com")),
                                now.truncatedTo(MILLIS)
                            )
                        )

                    expectNoEvents()

                    launch {
                        rSocketRequester.route("api.v1.messages.stream")
                            .dataWithType(flow {
                                emit(
                                    MessageViewModel(
                                        "`message4`",
                                        UserViewModel("user", URL("http://user1.com")),
                                        now.plusSeconds(1)
                                    )
                                )
                            })
                            .retrieveFlow<Void>()
                            .collect()
                    }

                    assertThat(expectItem().prepareForTesting())
                        .isEqualTo(
                            MessageViewModel(
                                "<body><p><code>message4</code></p></body>",
                                UserViewModel("user", URL("http://user1.com")),
                                now.plusSeconds(1).truncatedTo(MILLIS)
                            )
                        )

                    cancelAndIgnoreRemainingEvents()
                }
        }
    }

    @ExperimentalTime
    @Test
    fun `test that messages streamed to the API is stored`() {
        runBlocking {
            launch {
                val rSocketRequester = rsocketBuilder.websocket(URI("ws://localhost:${serverPort}/rsocket"))

                rSocketRequester.route("api.v1.messages.stream")
                    .dataWithType(flow {
                        emit(
                            MessageViewModel(
                                "`message4`",
                                UserViewModel("user", URL("http://user1.com")),
                                now.plusSeconds(1)
                            )
                        )
                    })
                    .retrieveFlow<Void>()
                    .collect()
            }

            delay(2.seconds)

            messageRepository.findAll()
                .first { it.content.contains("message4") }
                .apply {
                    assertThat(this.prepareForTesting())
                        .isEqualTo(
                            Message(
                                "`message4`",
                                ContentType.MARKDOWN,
                                now.plusSeconds(1).truncatedTo(MILLIS),
                                "user",
                                "http://user1.com"
                            )
                        )
                }
        }
    }
}
