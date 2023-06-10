package com.example.kotlin.chat.repository

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.query.Param

interface MessageRepository : CoroutineCrudRepository<Message, String> {

    // language=SQL
    @Query("""
        select * from (
            select * from messages
            order by sent desc
            limit 10
        ) order by sent
    """)
    suspend fun findLatest(): List<Message>

    // language=SQL
    @Query("""
        select * from (
            select * from messages
            where sent > (select sent from messages where id = :id)
            order by sent desc 
        ) order by sent
    """)
    suspend fun findLatest(@Param("id") id: String): List<Message>
}
