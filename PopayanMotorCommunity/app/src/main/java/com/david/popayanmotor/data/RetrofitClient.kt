package com.david.popayanmotor.data

import com.david.popayanmotor.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Sale de api.baseUrl en local.properties (ver app/build.gradle.kts); sin ella usa el emulador.
    private const val BASE_URL = BuildConfig.API_BASE_URL

    // Agrega "Authorization: Bearer <token>" a todas las peticiones si hay sesión.
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = SessionManager.token
            val request = if (token != null) {
                chain.request().newBuilder().header("Authorization", "Bearer $token").build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
