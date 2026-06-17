package com.fabbixmb.app.data.remote.dto

data class ZabbixRequest(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: Map<String, Any> = emptyMap(),
    val id: Int = 1,
    val auth: String? = null
)

data class ZabbixResponse<T>(
    val jsonrpc: String? = null,
    val id: Int? = null,
    val result: T? = null,
    val error: ZabbixError? = null
)

data class ZabbixError(
    val code: Int,
    val message: String,
    val data: String? = null
)
