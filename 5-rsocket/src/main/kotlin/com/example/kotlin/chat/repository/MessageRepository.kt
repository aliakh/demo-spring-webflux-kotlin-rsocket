package com.example.kotlin.chat.repository

import kotlinx.coroutines.flow.Flow
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
    fun findLatest(): Flow<Message>

    // language=SQL
    @Query("""
        select * from (
            select * from messages
            where sent > (select sent from messages where id = :id)
            order by sent desc 
        ) order by sent
    """)
    fun findLatest(@Param("id") id: String): Flow<Message>
}
