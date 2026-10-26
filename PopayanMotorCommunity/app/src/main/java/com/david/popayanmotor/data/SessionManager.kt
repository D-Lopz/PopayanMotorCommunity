package com.david.popayanmotor.data

import android.content.Context

/**
 * Guarda la sesión (token JWT, nombre y rol) en SharedPreferences,
 * para no pedir login cada vez que se abre la app.
 */
object SessionManager {

    private const val PREFS = "session"
    private const val KEY_TOKEN = "token"
    private const val KEY_ID = "id"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_ROLE = "role"

    // Se guarda en memoria para que el interceptor de Retrofit lo lea sin Context.
    var token: String? = null
        private set

    fun init(context: Context) {
        token = prefs(context).getString(KEY_TOKEN, null)
    }

    fun save(context: Context, response: TokenResponse) {
        token = response.accessToken
        prefs(context).edit()
            .putString(KEY_TOKEN, response.accessToken)
            .putInt(KEY_ID, response.member.id)
            .putString(KEY_NAME, response.member.name)
            .putString(KEY_EMAIL, response.member.email)
            .putString(KEY_ROLE, response.member.role)
            .apply()
    }

    /** null en sesiones guardadas antes de la v1.2 (no tenían id): hay que volver a iniciar sesión. */
    fun id(context: Context) = prefs(context).getInt(KEY_ID, -1).takeIf { it != -1 }
    fun name(context: Context) = prefs(context).getString(KEY_NAME, "") ?: ""

    /** Tras editar el perfil, para que el saludo de Inicio use el nombre nuevo. */
    fun updateName(context: Context, name: String) = prefs(context).edit().putString(KEY_NAME, name).apply()
    fun email(context: Context) = prefs(context).getString(KEY_EMAIL, "") ?: ""
    fun isAdmin(context: Context) = prefs(context).getString(KEY_ROLE, null) == "ADMINISTRATOR"

    fun clear(context: Context) {
        token = null
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
