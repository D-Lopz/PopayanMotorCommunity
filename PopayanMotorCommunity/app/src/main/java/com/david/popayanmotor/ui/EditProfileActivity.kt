package com.david.popayanmotor.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.david.popayanmotor.R
import com.david.popayanmotor.data.ApiError
import com.david.popayanmotor.data.Member
import com.david.popayanmotor.data.MemberUpdateRequest
import com.david.popayanmotor.data.RetrofitClient
import com.david.popayanmotor.data.SessionManager
import com.david.popayanmotor.databinding.ActivityEditProfileBinding
import kotlinx.coroutines.launch

/** Editar nombre, celular y ciudad (PUT /api/members/{id}). El backend solo deja editar el propio perfil. */
class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var memberId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)  // por si Android recreó la app directamente en esta pantalla
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = SessionManager.id(this)
        if (id == null) {  // sesión antigua sin id
            LoginActivity.restart(this)
            return
        }
        memberId = id
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnSave.setOnClickListener { save() }

        // Al rotar, los campos se restauran solos; solo se cargan del backend la primera vez.
        if (savedInstanceState == null) load() else binding.form.isVisible = true
    }

    private fun load() {
        lifecycleScope.launch {
            setLoading(true)
            try {
                fill(RetrofitClient.api.getMember(memberId))
                binding.form.isVisible = true
            } catch (e: Exception) {
                fail(e)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun fill(m: Member) {
        binding.tvInitials.text = Format.initials(m.name)
        binding.etName.setText(m.name)
        binding.etPhone.setText(m.phone.orEmpty())
        binding.etCity.setText(m.city.orEmpty())
    }

    private fun save() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val city = binding.etCity.text.toString().trim()

        // Mismas reglas que el registro y que MemberUpdate en el backend (que vuelve a validar).
        binding.tilName.error = if (name.length in 2..100) null else getString(R.string.error_name)
        binding.tilPhone.error = if (RegisterForm.isValidPhone(phone)) null else getString(R.string.error_phone)
        if (binding.tilName.error != null || binding.tilPhone.error != null) return

        lifecycleScope.launch {
            setLoading(true)
            try {
                val saved = RetrofitClient.api.updateMember(
                    memberId, MemberUpdateRequest(name, phone.ifEmpty { null }, city.ifEmpty { null })
                )
                SessionManager.updateName(this@EditProfileActivity, saved.name)  // saludo de Inicio
                Toast.makeText(this@EditProfileActivity, R.string.profile_saved, Toast.LENGTH_SHORT).show()
                finish()  // el perfil recarga en onResume
            } catch (e: Exception) {
                fail(e)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun fail(e: Exception) {
        if (ApiError.isUnauthorized(e)) {
            LoginActivity.restart(this)
            return
        }
        binding.tvError.text = ApiError.message(e)
        binding.tvError.isVisible = true
        binding.form.isVisible = true  // para que el error y "Guardar" se vean aunque la carga haya fallado
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.isVisible = loading
        binding.btnSave.isEnabled = !loading
        if (loading) binding.tvError.isVisible = false
    }
}
