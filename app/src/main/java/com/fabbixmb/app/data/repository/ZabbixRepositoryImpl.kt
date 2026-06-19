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
            "suppressed" to false,
            "limit" to 200
        )
        if (severities != null) params["severities"] = severities
        val request = ZabbixRequest(method = "problem.get", params = params)
        val response = service.call(request, "Bearer $auth")
        if (response.error != null) throw ZabbixException(response.error.message, response.error.data)
        val problems = parseProblems(response.result)

        // Batch-fetch host names and trigger details via trigger.get
        val triggerIds = problems.map { it.objectId }.distinct()
        if (triggerIds.isEmpty()) return@apiCall problems

        val triggerParams = mapOf<String, Any>(
            "triggerids" to triggerIds,
            "selectHosts" to listOf("hostid", "name", "active_available"),
            "output" to listOf("triggerid", "description", "expression", "opdata"),
            "monitored" to true
        )
        val triggerRequest = ZabbixRequest(method = "trigger.get", params = triggerParams)
        val triggerResponse = service.call(triggerRequest, "Bearer $auth")
        val triggerHostMap = parseTriggerHosts(triggerResponse.result)

        // Получаем дополнительные данные о хостах (интерфейсы и группы)
        val allHostIds = triggerHostMap.values.flatMap { it.hosts.map { h -> h.hostId } }.distinct()
        if (allHostIds.isEmpty()) return@apiCall problems
            .filter { triggerHostMap.containsKey(it.objectId) }
            .map { problem ->
                val triggerInfo = triggerHostMap[problem.objectId]
                problem.copy(
                    hosts = triggerInfo?.hosts ?: emptyList(),
                    triggerDescription = triggerInfo?.triggerDescription ?: "",
                    triggerExpression = triggerInfo?.triggerExpression ?: "",
                    opdata = triggerInfo?.opdata ?: ""
                )
            }

        // Вызов host.get для получения интерфейсов и групп
        val hostDetailParams = mapOf<String, Any>(
            "hostids" to allHostIds,
            "output" to listOf("hostid"),
            "selectInterfaces" to listOf("ip", "dns", "type"),
            "selectHostGroups" to listOf("groupid", "name")
        )
        val hostDetailRequest = ZabbixRequest(method = "host.get", params = hostDetailParams)
        val hostDetailResponse = service.call(hostDetailRequest, "Bearer $auth")
        val hostDetails = parseHostDetails(hostDetailResponse.result)  // Map<hostId, HostExtraInfo>

        problems
            .filter { triggerHostMap.containsKey(it.objectId) }
            .map { problem ->
                val triggerInfo = triggerHostMap[problem.objectId]
                val hosts = triggerInfo?.hosts?.map { host ->
                    val extra = hostDetails[host.hostId]
                    host.copy(
                        ip = extra?.ip ?: "",
                        groups = extra?.groups ?: emptyList()
                    )
                } ?: emptyList()
                problem.copy(
                    hosts = hosts,
                    triggerDescription = triggerInfo?.triggerDescription ?: "",
                    triggerExpression = triggerInfo?.triggerExpression ?: "",
                    opdata = triggerInfo?.opdata ?: ""
                )
            }
    }

    override suspend fun getHosts(
        baseUrl: String, ignoreSsl: Boolean, auth: String
    ): Result<List<Host>> = apiCall(baseUrl, ignoreSsl) { service ->
        val params = mapOf(
            "output" to listOf("hostid", "host", "name", "status", "active_available", "proxy_hostid", "proxyid"),
            "selectInterfaces" to listOf("ip", "dns", "type"),
            "selectParentTemplates" to listOf("templateid", "name"),
            "selectMacros" to listOf("macro", "value"),
            "selectHostGroups" to listOf("groupid", "name"),
            "sortfield" to "name",
            "limit" to 500
        )
        val request = ZabbixRequest(method = "host.get", params = params)
        val response = service.call(request, "Bearer $auth")
        if (response.error != null) throw ZabbixException(response.error.message, response.error.data)
        
        // Получаем имена прокси
        val proxyIds = parseProxyIds(response.result)
        val proxyNames = if (proxyIds.isNotEmpty()) {
            val proxyParams = mapOf<String, Any>(
                "proxyids" to proxyIds,
                "output" to listOf("proxyid", "name")
            )
            val proxyRequest = ZabbixRequest(method = "proxy.get", params = proxyParams)
            val proxyResponse = service.call(proxyRequest, "Bearer $auth")
            parseProxyNames(proxyResponse.result)
        } else emptyMap()
        
        parseHosts(response.result, proxyNames)
    }

    override suspend fun getDashboards(
        baseUrl: String, ignoreSsl: Boolean, auth: String
    ): Result<List<Dashboard>> = apiCall(baseUrl, ignoreSsl) { service ->
        val params = mapOf("output" to "extend")
        val request = ZabbixRequest(method = "dashboard.get", params = params)
        val response = service.call(request, "Bearer $auth")
        if (response.error != null) throw ZabbixException(response.error.message, response.error.data)
        parseDashboards(response.result)
    }

    override suspend fun getTriggers(
        baseUrl: String, ignoreSsl: Boolean, auth: String
    ): Result<List<Trigger>> = apiCall(baseUrl, ignoreSsl) { service ->
        val params = mapOf(
            "output" to "extend",
            "selectHosts" to listOf("hostid", "name"),
            "sortfield" to "lastchange",
            "sortorder" to "DESC",
            "monitored" to true,
            "limit" to 200
        )
        val request = ZabbixRequest(method = "trigger.get", params = params)
        val response = service.call(request, "Bearer $auth")
        if (response.error != null) throw ZabbixException(response.error.message, response.error.data)
        parseTriggers(response.result)
    }

    override suspend fun testConnection(
        baseUrl: String, ignoreSsl: Boolean
    ): Result<String> = apiCall(baseUrl, ignoreSsl) { service ->
        val request = ZabbixRequest(method = "apiinfo.version", params = emptyMap())
        val response = service.call(request)
        if (response.error != null) throw ZabbixException(response.error.message, response.error.data)
        (response.result as? JsonPrimitive)?.asString ?: "unknown"
    }

    data class TriggerHostInfo(
        val hosts: List<ProblemHost>,
        val triggerDescription: String,
        val triggerExpression: String,
        val opdata: String
    )

    data class HostExtraInfo(
        val ip: String,
        val groups: List<String>
    )

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

    private fun parseHosts(result: JsonElement?, proxyNames: Map<String, String> = emptyMap()): List<Host> {
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
            
            val templates = (obj.get("parentTemplates") as? JsonArray)?.mapNotNull {
                it.asJsonObject.get("name")?.asString
            } ?: emptyList()
            
            val macros = (obj.get("macros") as? JsonArray)?.mapNotNull {
                val m = it.asJsonObject
                val macro = m.get("macro")?.asString ?: return@mapNotNull null
                val value = m.get("value")?.asString ?: ""
                HostMacro(macro, value)
            } ?: emptyList()
            
            val proxyId = obj.get("proxyid")?.asString 
                ?: obj.get("proxy_hostid")?.asString ?: "0"
            val monitoredBy = if (proxyId == "0" || proxyId.isEmpty()) "Zabbix Server"
                else proxyNames[proxyId] ?: "Proxy #$proxyId"
            
            Host(
                hostId = obj.get("hostid")?.asString ?: "",
                technicalName = obj.get("host")?.asString ?: "",
                visibleName = obj.get("name")?.asString ?: "",
                enabled = obj.get("status")?.asString == "0",
                available = HostAvailability.fromId(obj.get("active_available")?.asString?.toIntOrNull() ?: 0),
                interfaces = interfaces,
                templates = templates,
                macros = macros,
                monitoredBy = monitoredBy
            )
        }
    }

    private fun parseProxyIds(result: JsonElement?): List<String> {
        val arr = result as? JsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el.asJsonObject
            val proxyId = obj.get("proxyid")?.asString 
                ?: obj.get("proxy_hostid")?.asString ?: "0"
            if (proxyId != "0" && proxyId.isNotEmpty()) proxyId else null
        }.distinct()
    }

    private fun parseProxyNames(result: JsonElement?): Map<String, String> {
        val arr = result as? JsonArray ?: return emptyMap()
        return arr.associate { el ->
            val obj = el.asJsonObject
            val proxyId = obj.get("proxyid")?.asString ?: ""
            val name = obj.get("name")?.asString ?: ""
            proxyId to name
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

    private fun parseTriggerHosts(result: JsonElement?): Map<String, TriggerHostInfo> {
        val arr = result as? JsonArray ?: return emptyMap()
        return arr.associate { el ->
            val obj = el.asJsonObject
            val triggerId = obj.get("triggerid")?.asString ?: ""
            val hosts = (obj.get("hosts") as? JsonArray)?.mapNotNull { h ->
                val hostObj = h.asJsonObject
                val hostId = hostObj.get("hostid")?.asString ?: ""
                val name = hostObj.get("name")?.asString ?: ""
                val availability = hostObj.get("active_available")?.asString?.toIntOrNull() ?: 0
                ProblemHost(
                    hostId = hostId,
                    name = name,
                    availability = HostAvailability.fromId(availability)
                )
            } ?: emptyList()
            
            val triggerDescription = obj.get("description")?.asString ?: ""
            val triggerExpression = obj.get("expression")?.asString ?: ""
            val opdata = obj.get("opdata")?.asString ?: ""
            
            triggerId to TriggerHostInfo(
                hosts = hosts,
                triggerDescription = triggerDescription,
                triggerExpression = triggerExpression,
                opdata = opdata
            )
        }
    }

    private fun parseHostDetails(result: JsonElement?): Map<String, HostExtraInfo> {
        val arr = result as? JsonArray ?: return emptyMap()
        return arr.associate { el ->
            val obj = el.asJsonObject
            val hostId = obj.get("hostid")?.asString ?: ""
            val ip = (obj.get("interfaces") as? JsonArray)
                ?.firstOrNull()?.asJsonObject?.get("ip")?.asString ?: ""
            val groups = (obj.getAsJsonArray("hostgroups") ?: obj.getAsJsonArray("groups"))
                ?.mapNotNull { it.asJsonObject.get("name")?.asString } ?: emptyList()
            hostId to HostExtraInfo(ip = ip, groups = groups)
        }
    }

    private fun parseTriggers(result: JsonElement?): List<Trigger> {
        val arr = result as? JsonArray ?: return emptyList()
        return arr.map { el ->
            val obj = el.asJsonObject
            val hosts = (obj.get("hosts") as? JsonArray)?.mapNotNull { h ->
                h.asJsonObject.get("name")?.asString
            } ?: emptyList()
            Trigger(
                triggerId = obj.get("triggerid")?.asString ?: "",
                description = obj.get("description")?.asString ?: "",
                priority = Severity.fromId(obj.get("priority")?.asString?.toIntOrNull() ?: 0),
                status = obj.get("status")?.asString == "0",
                hosts = hosts
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