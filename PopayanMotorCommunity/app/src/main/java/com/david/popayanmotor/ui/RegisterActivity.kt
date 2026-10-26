package com.david.popayanmotor.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.david.popayanmotor.R
import com.david.popayanmotor.data.ApiError
import com.david.popayanmotor.data.LoginRequest
import com.david.popayanmotor.data.RegisterRequest
import com.david.popayanmotor.data.RetrofitClient
import com.david.popayanmotor.data.SessionManager
import com.david.popayanmotor.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

/** Reglas del formulario de registro (sin Android, para poder probarlas con JUnit). */
object RegisterForm {

    // Debe coincidir con EMAIL_DOMAIN de app/schemas/member.py (el backend vuelve a validarlo).
    const val EMAIL_DOMAIN = "@popayanmotor.com"

    private val USER_RE = Regex("^[a-z0-9]+([._-][a-z0-9]+)*$")
    private val PHONE_RE = Regex("^\\+?\\d{7,15}$")

    /** "David.Lopez" → "david.lopez@popayanmotor.com"; null si el usuario no es válido. */
    fun email(user: String): String? {
        val clean = user.trim().lowercase().removeSuffix(EMAIL_DOMAIN)  // por si escribe el correo completo
        return if (USER_RE.matches(clean)) clean + EMAIL_DOMAIN else null
    }

    /** Vacío = sin celular (es opcional). Mismas reglas que el backend: 7 a 15 dígitos. */
    fun isValidPhone(phone: String) = phone.isEmpty() || PHONE_RE.matches(phone.replace(" ", ""))

    fun isValidPassword(password: String) = password.length in 8..64
}

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tilEmail.suffixText = RegisterForm.EMAIL_DOMAIN  // el dominio se ve fijo al final del campo
        binding.btnRegister.setOnClickListener { register() }
        binding.btnGoLogin.setOnClickListener { finish() }  // vuelve al login que quedó debajo
    }

    private fun register() {
        val name = binding.etName.text.toString().trim()
        val email = RegisterForm.email(binding.etEmail.text.toString())
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirm.text.toString()

        // Validación en la app (rápida); el backend vuelve a validar todo.
        binding.tilName.error = if (name.length >= 2) null else getString(R.string.error_name)
        binding.tilEmail.error = if (email != null) null else getString(R.string.error_email_user)
        binding.tilPhone.error = if (RegisterForm.isValidPhone(phone)) null else getString(R.string.error_phone)
        binding.tilPassword.error =
            if (RegisterForm.isValidPassword(password)) null else getString(R.string.error_password_short)
        binding.tilConfirm.error = if (confirm == password) null else getString(R.string.error_password_match)
        val tils = listOf(binding.tilName, binding.tilEmail, binding.tilPhone, binding.tilPassword, binding.tilConfirm)
        if (email == null || tils.any { it.error != null }) return

        lifecycleScope.launch {
            setLoading(true)
            try {
                RetrofitClient.api.register(RegisterRequest(name, email, password, phone.ifEmpty { null }))
                // Register no devuelve token: se inicia sesión con los mismos datos para entrar directo.
                val session = RetrofitClient.api.login(LoginRequest(email, password))
                SessionManager.save(this@RegisterActivity, session)
                startActivity(
                    Intent(this@RegisterActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)  // cierra también el login
                )
            } catch (e: Exception) {
                binding.tvError.text = ApiError.message(e)  // ej: "Ya existe una cuenta con ese correo"
                binding.tvError.isVisible = true
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.isVisible = loading
        binding.btnRegister.isEnabled = !loading
        if (loading) binding.tvError.isVisible = false
    }
}
