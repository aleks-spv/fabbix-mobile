package com.fabbixmb.app.domain.repository

import com.fabbixmb.app.data.local.ServerEntity
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    suspend fun add(server: ServerEntity): Long
    suspend fun update(server: ServerEntity)
    suspend fun delete(server: ServerEntity)
    fun getAll(): Flow<List<ServerEntity>>
    suspend fun getById(id: Int): ServerEntity?
}