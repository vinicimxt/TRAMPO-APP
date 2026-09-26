package com.example.trampo.data.api

import android.util.Log
import com.example.trampo.data.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val session: TokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val token =
            if (request.url.encodedPath in setOf("/api/v1/auth/login", "/api/v1/auth/register")) {
                null
            } else {
                session.token()
            }

        val authenticated = request.newBuilder().apply {
            removeHeader("Authorization")

            if (!token.isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
        }.build()


        try {
            val response = chain.proceed(authenticated)


            if (response.code == 401 && token != null) {
                session.clearIfMatches(token)
            }

            return response
        } catch (e: Exception) {

            throw e
        }
    }
}