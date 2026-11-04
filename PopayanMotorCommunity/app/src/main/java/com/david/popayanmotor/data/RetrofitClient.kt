package com.david.popayanmotor.data

import com.david.popayanmotor.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // debug: api.baseUrl (o el emulador); release: api.releaseUrl. Ver app/build.gradle.kts.
    private const val BASE_URL = BuildConfig.API_BASE_URL

    // Agrega "Authorization: Bearer <token>" a todas las peticiones si hay sesión.
    // Timeouts largos: el plan gratis de Azure duerme la app y la primera petición puede tardar ~1 min.
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
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
