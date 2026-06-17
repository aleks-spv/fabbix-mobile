package com.fabbixmb.app.domain.repository

import com.fabbixmb.app.domain.model.Dashboard
import com.fabbixmb.app.domain.model.Host
import com.fabbixmb.app.domain.model.Problem

interface ZabbixRepository {
    suspend fun login(baseUrl: String, ignoreSsl: Boolean, user: String, password: String): Result<String>
    suspend fun getProblems(baseUrl: String, ignoreSsl: Boolean, auth: String, severities: List<Int>? = null): Result<List<Problem>>
    suspend fun getHosts(baseUrl: String, ignoreSsl: Boolean, auth: String): Result<List<Host>>
    suspend fun getDashboards(baseUrl: String, ignoreSsl: Boolean, auth: String): Result<List<Dashboard>>
    suspend fun testConnection(baseUrl: String, ignoreSsl: Boolean): Result<String>
}