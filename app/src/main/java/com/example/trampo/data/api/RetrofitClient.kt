package com.example.trampo.data.api

import com.example.trampo.BuildConfig
import com.example.trampo.data.TokenStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    fun create(session: TokenStore): TrampoApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            // Sem retry transparente para reservas e sem log de conteúdo sensível.
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .addInterceptor(AuthInterceptor(session))
            .build())
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(TrampoApi::class.java)
}