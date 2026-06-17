package com.fabbixmb.app.data.remote

import com.fabbixmb.app.data.remote.dto.ZabbixRequest
import com.fabbixmb.app.data.remote.dto.ZabbixResponse
import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST

interface ZabbixApiService {
    @POST("api_jsonrpc.php")
    suspend fun call(@Body request: ZabbixRequest): ZabbixResponse<JsonElement>
}
