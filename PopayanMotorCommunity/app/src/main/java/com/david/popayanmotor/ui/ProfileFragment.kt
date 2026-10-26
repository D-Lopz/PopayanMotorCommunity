package com.david.popayanmotor.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.david.popayanmotor.R
import com.david.popayanmotor.data.ApiError
import com.david.popayanmotor.data.Member
import com.david.popayanmotor.data.RetrofitClient
import com.david.popayanmotor.data.SessionManager
import com.david.popayanmotor.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

/** Perfil (captura 2): datos del backend, estadísticas reales y accesos según el rol. */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        val main = requireActivity() as MainActivity
        binding.btnEdit.setOnClickListener { startActivity(Intent(ctx, EditProfileActivity::class.java)) }
        binding.rowGarage.setOnClickListener { main.selectTab(R.id.nav_garage) }
        binding.rowAdmin.setOnClickListener { main.selectTab(R.id.nav_events) }
        binding.btnRetry.setOnClickListener { load() }
        binding.tvVersion.text = getString(
            R.string.app_version, ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
        )
        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(ctx)
                .setMessage(R.string.logout_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout) { _, _ -> main.logout() }
                .show()
        }
    }

    // Se recarga al volver de "Editar perfil" (y al volver a la pestaña) para mostrar datos frescos.
    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val id = SessionManager.id(requireContext())!!  // MainActivity garantiza el id
        viewLifecycleOwner.lifecycleScope.launch {
            showState(loading = true)
            try {
                // Las tres consultas van en paralelo; todas filtradas al propio miembro.
                // coroutineScope: si una falla, el error llega a este catch en vez de cerrar la app.
                val (member, vehicles, registrations) = coroutineScope {
                    val m = async { RetrofitClient.api.getMember(id) }
                    val v = async { RetrofitClient.api.getVehicles(id) }
                    val r = async { RetrofitClient.api.getRegistrations(id) }
                    Triple(m.await(), v.await(), r.await())
                }
                show(member, vehicles.size, registrations.size, Format.kmInRoutes(registrations, Format.nowColombia()))
                showState(content = true)
            } catch (e: Exception) {
                if (ApiError.isUnauthorized(e)) {
                    (requireActivity() as MainActivity).logout()  // token vencido → volver al login
                    return@launch
                }
                binding.tvError.text = ApiError.message(e)
                showState(error = true)
            }
        }
    }

    private fun show(member: Member, vehicles: Int, events: Int, km: Double) {
        binding.tvInitials.text = Format.initials(member.name)
        binding.tvName.text = member.name
        val since = getString(R.string.member_since, Format.monthYear(member.joinDate))
        binding.tvSince.text = listOfNotNull(member.city, since).joinToString(" · ")
        // El rol sale del backend, no de la sesión: la insignia y la opción de admin solo para ADMINISTRATOR.
        val isAdmin = member.role == "ADMINISTRATOR"
        binding.tvAdminBadge.isVisible = isAdmin
        binding.rowAdmin.isVisible = isAdmin
        binding.tvEvents.text = String.format(Locale.US, "%02d", events)
        binding.tvVehicles.text = String.format(Locale.US, "%02d", vehicles)
        binding.tvKm.text = Format.km(km)
    }

    private fun showState(loading: Boolean = false, error: Boolean = false, content: Boolean = false) {
        binding.progress.isVisible = loading
        binding.errorGroup.isVisible = error
        binding.content.isVisible = content
        binding.btnEdit.isEnabled = content  // no se edita hasta tener los datos cargados
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
