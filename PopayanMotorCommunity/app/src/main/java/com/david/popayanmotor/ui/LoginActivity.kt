package com.david.popayanmotor.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import kotlin.random.Random
import com.david.popayanmotor.R
import com.david.popayanmotor.data.ApiError
import com.david.popayanmotor.data.LoginRequest
import com.david.popayanmotor.data.RetrofitClient
import com.david.popayanmotor.data.SessionManager
import com.david.popayanmotor.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

/** Regla del correo del login (sin Android, para probarla con JUnit). */
object LoginForm {
    /**
     * "david" → "david@popayanmotor.com". Si ya trae "@" se envía tal cual: así no se duplica el dominio
     * y las cuentas antiguas con otros dominios siguen entrando. El backend valida el correo igual que antes.
     */
    fun email(text: String): String {
        val t = text.trim()
        return if (t.isEmpty() || "@" in t) t else t + RegisterForm.EMAIL_DOMAIN
    }
}

/** Foto del login (sin Android, para probarla con JUnit). */
object LoginHero {
    /**
     * Índice de la foto a mostrar entre [count]: al azar y distinto de [last] si hay más de una.
     * null sin fotos. Si [last] ya no existe (se quitaron fotos) se elige entre todas.
     */
    fun next(count: Int, last: Int, random: Random = Random.Default): Int? = when {
        count <= 0 -> null
        count == 1 -> 0
        last !in 0 until count -> random.nextInt(count)
        else -> random.nextInt(count - 1).let { if (it >= last) it + 1 else it }  // salta la anterior
    }
}

class LoginActivity : AppCompatActivity() {

    companion object {
        // Fuera de la sesión: SessionManager.clear() al cerrar sesión no debe olvidar la última foto.
        private const val HERO_PREFS = "login_ui"
        private const val KEY_LAST_HERO = "last_hero"

        /** Token vencido en una pantalla de formulario: borra la sesión y vuelve al login sin dejar pantallas atrás. */
        fun restart(activity: Activity) {
            SessionManager.clear(activity)
            activity.startActivity(
                Intent(activity, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)

        // Si ya hay sesión guardada, se salta el login.
        if (SessionManager.token != null) {
            goToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        showHero(newOpening = savedInstanceState == null)  // al rotar se conserva la misma foto

        // El dominio se ve al final del campo mientras el texto no tenga "@" (al escribir, borrar o pegar).
        binding.etEmail.doAfterTextChanged { text ->
            val suffix = if (text?.contains('@') == true) null else RegisterForm.EMAIL_DOMAIN
            // Solo al cambiar: reasignar el sufijo en cada tecla vuelve a medir el campo y hace lento escribir.
            if (binding.tilEmail.suffixText?.toString() != suffix) binding.tilEmail.suffixText = suffix
        }
        binding.tilEmail.suffixText = RegisterForm.EMAIL_DOMAIN
        binding.btnLogin.setOnClickListener { login() }
        binding.btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun showHero(newOpening: Boolean) {
        val heroes = resources.obtainTypedArray(R.array.login_heroes)
        try {
            val prefs = getSharedPreferences(HERO_PREFS, MODE_PRIVATE)
            val last = prefs.getInt(KEY_LAST_HERO, -1)
            val index = (if (newOpening) LoginHero.next(heroes.length(), last) else last.takeIf { it in 0 until heroes.length() })
                ?: return  // sin fotos: queda el degradado
            binding.ivHero.setImageResource(heroes.getResourceId(index, 0))
            binding.ivHero.isVisible = true
            prefs.edit().putInt(KEY_LAST_HERO, index).apply()
        } finally {
            heroes.recycle()
        }
    }

    private fun login() {
        val email = LoginForm.email(binding.etEmail.text.toString())
        val password = binding.etPassword.text.toString()

        // Validación en la app (rápida); el backend vuelve a validar.
        binding.tilEmail.error =
            if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) null else getString(R.string.error_email)
        binding.tilPassword.error =
            if (password.isNotEmpty()) null else getString(R.string.error_password)
        if (binding.tilEmail.error != null || binding.tilPassword.error != null) return

        lifecycleScope.launch {
            setLoading(true)
            try {
                val response = RetrofitClient.api.login(LoginRequest(email, password))
                SessionManager.save(this@LoginActivity, response)
                goToMain()
            } catch (e: Exception) {
                binding.tvError.text = ApiError.message(e)  // ej: "Correo o contraseña incorrectos"
                binding.tvError.isVisible = true
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.isVisible = loading
        binding.btnLogin.isEnabled = !loading
        if (loading) binding.tvError.isVisible = false
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()  // para que "atrás" no regrese al login
    }
}
