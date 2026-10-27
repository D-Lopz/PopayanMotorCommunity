package com.david.popayanmotor.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.david.popayanmotor.R
import com.david.popayanmotor.data.ApiError
import com.david.popayanmotor.data.Registration
import com.david.popayanmotor.data.RetrofitClient
import com.david.popayanmotor.data.SessionManager
import com.david.popayanmotor.databinding.ActivityRegistrationsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Mis inscripciones (GET /api/registrations?member_id=propio): próximas primero, con opción de cancelar. */
class RegistrationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationsBinding
    private val adapter = RegistrationAdapter(onCancel = ::confirmCancel)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvRegistrations.layoutManager = LinearLayoutManager(this)
        binding.rvRegistrations.adapter = adapter
        binding.btnRetry.setOnClickListener { load() }
        load()
    }

    private fun load() {
        val id = SessionManager.id(this) ?: return LoginActivity.restart(this)
        lifecycleScope.launch {
            showState(loading = true)
            try {
                val now = Format.nowColombia()
                val registrations = Format.sortRegistrations(RetrofitClient.api.getRegistrations(id), now)
                adapter.submit(registrations, now)
                if (registrations.isEmpty()) showState(empty = true) else showState(content = true)
            } catch (e: Exception) {
                handleError(e) { binding.tvError.text = it; showState(error = true) }
            }
        }
    }

    private fun confirmCancel(registration: Registration) {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.cancel_registration_confirm, registration.event.name))
            .setNegativeButton(R.string.back, null)
            .setPositiveButton(R.string.cancel_registration) { _, _ -> cancel(registration) }
            .show()
    }

    private fun cancel(registration: Registration) {
        lifecycleScope.launch {
            showState(loading = true)
            try {
                val response = RetrofitClient.api.deleteRegistration(registration.id)
                if (!response.isSuccessful) throw HttpException(response)  // ej. 409: el evento ya empezó
                Toast.makeText(this@RegistrationsActivity, R.string.registration_cancelled, Toast.LENGTH_SHORT).show()
                load()
            } catch (e: Exception) {
                // No se canceló: se avisa por qué y se vuelve a mostrar la lista.
                handleError(e) { message ->
                    MaterialAlertDialogBuilder(this@RegistrationsActivity).setMessage(message).setPositiveButton(android.R.string.ok, null).show()
                    load()
                }
            }
        }
    }

    private fun handleError(e: Exception, show: (String) -> Unit) {
        if (ApiError.isUnauthorized(e)) LoginActivity.restart(this) else show(ApiError.message(e))
    }

    private fun showState(loading: Boolean = false, error: Boolean = false, empty: Boolean = false, content: Boolean = false) {
        binding.progress.isVisible = loading
        binding.errorGroup.isVisible = error
        binding.tvEmpty.isVisible = empty
        binding.rvRegistrations.isVisible = content
    }
}
