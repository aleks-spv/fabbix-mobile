package com.fabbixmb.app.data.remote

import com.fabbixmb.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.util.concurrent.ConcurrentHashMap

class ZabbixApiClientFactory {
    private val cache = ConcurrentHashMap<Pair<String, Boolean>, ZabbixApiService>()

    fun createService(baseUrl: String, ignoreSsl: Boolean): ZabbixApiService {
        val key = baseUrl to ignoreSsl
        return cache.getOrPut(key) { buildService(baseUrl, ignoreSsl) }
    }

    private fun buildService(baseUrl: String, ignoreSsl: Boolean): ZabbixApiService {
        val builder = OkHttpClient.Builder()

        if (ignoreSsl) {
            val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            })
            val sslContext = SSLContext.getInstance("TLS").also {
                it.init(null, trustAll, SecureRandom())
            }
            builder.sslSocketFactory(sslContext.socketFactory, trustAll[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        }

        builder.addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            }
        )

        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .client(builder.build())
            .baseUrl(normalizedUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ZabbixApiService::class.java)
    }
}