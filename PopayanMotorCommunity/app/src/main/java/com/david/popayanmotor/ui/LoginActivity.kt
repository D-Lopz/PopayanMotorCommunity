package com.david.popayanmotor.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.david.popayanmotor.R
import com.david.popayanmotor.data.ApiError
import com.david.popayanmotor.data.LoginRequest
import com.david.popayanmotor.data.RetrofitClient
import com.david.popayanmotor.data.SessionManager
import com.david.popayanmotor.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    companion object {
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

        binding.btnLogin.setOnClickListener { login() }
        binding.btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login() {
        val email = binding.etEmail.text.toString().trim()
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
