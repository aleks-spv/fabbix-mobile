package com.fabbixmb.app.data.repository

import com.fabbixmb.app.data.local.ServerDao
import com.fabbixmb.app.data.local.ServerEntity
import com.fabbixmb.app.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepositoryImpl @Inject constructor(
    private val dao: ServerDao
) : ServerRepository {
    override suspend fun add(server: ServerEntity): Long = dao.insert(server)
    override suspend fun update(server: ServerEntity) = dao.update(server)
    override suspend fun delete(server: ServerEntity) = dao.delete(server)
    override fun getAll(): Flow<List<ServerEntity>> = dao.getAll()
    override suspend fun getById(id: Int): ServerEntity? = dao.getById(id)
}