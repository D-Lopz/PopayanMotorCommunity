package com.david.popayanmotor.data

import android.util.Log
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

/**
 * Convierte cualquier excepción de una llamada a la API en un mensaje para el usuario.
 * - HttpException: el backend respondió con error (4xx/5xx) → usamos su "detail".
 * - IOException: no hubo respuesta (servidor apagado, sin red, IP equivocada).
 */
object ApiError {

    fun message(e: Exception): String {
        Log.w("ApiError", "Fallo en la llamada a la API", e)  // detalle técnico en Logcat
        return userMessage(e)
    }

    private fun userMessage(e: Exception): String = when (e) {
        is HttpException -> fromBody(e.response()?.errorBody()?.string()) ?: "Error del servidor (${e.code()})"
        is IOException -> "No se pudo conectar con el servidor. Revisa que el backend esté encendido."
        else -> "Error inesperado: ${e.message}"
    }

    /** true si el token venció o ya no es válido → hay que volver a iniciar sesión. */
    fun isUnauthorized(e: Exception) = e is HttpException && e.code() == 401

    fun fromBody(json: String?): String? = try {
        Gson().fromJson(json, ApiErrorBody::class.java)?.detail
    } catch (e: Exception) {
        null  // el cuerpo no era JSON (ej. "Internal Server Error" en texto plano)
    }
}
