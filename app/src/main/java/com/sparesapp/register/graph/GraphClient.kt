package com.sparesapp.register.graph

import com.sparesapp.register.auth.AuthManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

private const val GRAPH_BASE_URL = "https://graph.microsoft.com/v1.0/"

class GraphAuthInterceptor(private val authManager: AuthManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        // Never attach our bearer token to a redirected pre-authenticated
        // download URL (different host) — OkHttp already strips
        // Authorization on cross-host redirects, this just avoids sending
        // it to graph.microsoft.com requests that don't need it either way.
        val token = runBlocking { authManager.acquireTokenSilent() }
        val authed = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authed)
    }
}

object GraphClient {
    fun build(authManager: AuthManager): GraphApi {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(GraphAuthInterceptor(authManager))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(GRAPH_BASE_URL)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        return retrofit.create(GraphApi::class.java)
    }
}
