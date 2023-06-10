package com.example.kotlin.chat.repository

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface MessageRepository : CrudRepository<Message, String> {

    // language=SQL
    @Query("""
        select * from (
            select * from messages
            order by sent desc
            limit 10
        ) order by sent
    """)
    fun findLatest(): List<Message>

    // language=SQL
    @Query("""
        select * from (
            select * from messages
            where sent > (select sent from messages where id = :id)
            order by sent desc 
        ) order by sent
    """)
    fun findLatest(@Param("id") id: String): List<Message>
}
