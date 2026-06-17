package com.fabbixmb.app.data.repository

import com.fabbixmb.app.data.remote.ZabbixApiClientFactory
import com.fabbixmb.app.data.remote.dto.ZabbixRequest
import com.fabbixmb.app.domain.model.*
import com.fabbixmb.app.domain.repository.ZabbixRepository
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZabbixRepositoryImpl @Inject constructor(
    private val factory: ZabbixApiClientFactory
) : ZabbixRepository {

    override suspend fun login(
        baseUrl: String, ignoreSsl: Boolean, user: String, password: String
    ): Result<String> = apiCall(baseUrl, ignoreSsl) { service ->
        val request = ZabbixRequest(
            method = "user.login",
            params = mapOf("username" to user, "password" to password)
        )
        val response = service.call(request)
        if (response.error != null) throw ZabbixException(response.error.message, response.error.data)
        val token = (response.result as? JsonPrimitive)?.asString
            ?: throw ZabbixException("Invalid login response")
        token
    }

    override suspend fun getProblems(
        baseUrl: String, ignoreSsl: Boolean, auth: String, severities: List<Int>?
    ): Result<List<Problem>> = apiCall(baseUrl, ignoreSsl) { service ->
        val params = mutableMapOf<String, Any>(
            "output" to "extend",
            "selectTags" to "extend",
            "sortfield" to listOf("eventid"),
            "sortorder" to "DESC",
            "recent" to true,
            "limit" to 200
        )
        if (severities != null) params["severities"] = severities
        val request = ZabbixRequest(method = "problem.get", params = params, auth = auth)
        val response = service.call(request)
        if (response.error != null) throw ZabbixException(response.error.message, response.error.data)
        parseProblems(response.result)
    }

    override suspend fun getHosts(
        baseUrl: String, ignoreSsl: Boolean, auth: String
    ): Result<List<Host>> = apiCall(baseUrl, ignoreSsl) { service ->
        val params = mapOf(
            "output" to listOf("hostid", "host", "name", "status", "active_available"),
            "selectInterfaces" to listOf("ip", "dns", "type"),
            "sortfield" to "name",
            "limit" to 500
        )
        val request = ZabbixRequest(method = "host.get", params = params, auth = auth)
        val response = service.call(request)
        if (response.error != null) throw ZabbixException(response.error.message, response.error.data)
        parseHosts(response.result)
    }

    override suspend fun getDashboards(
        baseUrl: String, ignoreSsl: Boolean, auth: String
    ): Result<List<Dashboard>> = apiCall(baseUrl, ignoreSsl) { service ->
        val params = mapOf("output" to "extend")
        val request = ZabbixRequest(method = "dashboard.get", params = params, auth = auth)
        val response = service.call(request)
        if (response.error != null) throw ZabbixException(response.error.message, response.error.data)
        parseDashboards(response.result)
    }

    override suspend fun testConnection(
        baseUrl: String, ignoreSsl: Boolean
    ): Result<String> = apiCall(baseUrl, ignoreSsl) { service ->
        val request = ZabbixRequest(method = "apiinfo.version", params = emptyMap())
        val response = service.call(request)
        if (response.error != null) throw ZabbixException(response.error.message, response.error.data)
        (response.result as? JsonPrimitive)?.asString ?: "unknown"
    }

    private fun parseProblems(result: JsonElement?): List<Problem> {
        val arr = result as? JsonArray ?: return emptyList()
        return arr.map { el ->
            val obj = el.asJsonObject
            Problem(
                eventId = obj.get("eventid")?.asString ?: "",
                objectId = obj.get("objectid")?.asString ?: "",
                name = obj.get("name")?.asString ?: "",
                severity = Severity.fromId(obj.get("severity")?.asString?.toIntOrNull() ?: 0),
                acknowledged = obj.get("acknowledged")?.asString == "1",
                clock = obj.get("clock")?.asString?.toLongOrNull() ?: 0L,
                resolvedClock = obj.get("r_clock")?.asString?.toLongOrNull()
            )
        }
    }

    private fun parseHosts(result: JsonElement?): List<Host> {
        val arr = result as? JsonArray ?: return emptyList()
        return arr.map { el ->
            val obj = el.asJsonObject
            val interfaces = (obj.get("interfaces") as? JsonArray)?.map { iface ->
                val io = iface.asJsonObject
                HostInterface(
                    ip = io.get("ip")?.asString ?: "",
                    dns = io.get("dns")?.asString ?: "",
                    type = io.get("type")?.asString?.toIntOrNull() ?: 0
                )
            } ?: emptyList()
            Host(
                hostId = obj.get("hostid")?.asString ?: "",
                technicalName = obj.get("host")?.asString ?: "",
                visibleName = obj.get("name")?.asString ?: "",
                enabled = obj.get("status")?.asString == "0",
                available = HostAvailability.fromId(obj.get("active_available")?.asString?.toIntOrNull() ?: 0),
                interfaces = interfaces
            )
        }
    }

    private fun parseDashboards(result: JsonElement?): List<Dashboard> {
        val arr = result as? JsonArray ?: return emptyList()
        return arr.map { el ->
            val obj = el.asJsonObject
            Dashboard(
                id = obj.get("dashboardid")?.asString ?: "",
                name = obj.get("name")?.asString ?: ""
            )
        }
    }

    private suspend fun <T> apiCall(
        baseUrl: String, ignoreSsl: Boolean, block: suspend (com.fabbixmb.app.data.remote.ZabbixApiService) -> T
    ): Result<T> = try {
        val service = factory.createService(baseUrl, ignoreSsl)
        Result.success(block(service))
    } catch (e: ZabbixException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

class ZabbixException(message: String, val detail: String? = null) : Exception(
    if (detail != null) "$message: $detail" else message
)