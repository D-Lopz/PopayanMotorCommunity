package com.david.popayanmotor.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.david.popayanmotor.R
import com.david.popayanmotor.data.ApiError
import com.david.popayanmotor.data.Event
import com.david.popayanmotor.data.RegistrationRequest
import com.david.popayanmotor.data.RetrofitClient
import com.david.popayanmotor.data.SessionManager
import com.david.popayanmotor.data.Vehicle
import com.david.popayanmotor.databinding.ActivityRegistrationFormBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Reglas del formulario de inscripción (sin Android, para probarlas con JUnit). */
object RegistrationForm {
    /** Igual que RegistrationIn del backend (companions: 0..10, no ocupan cupo). */
    val COMPANIONS = 0..10

    /** El principal primero (queda preseleccionado); el resto en el orden del backend. */
    fun sortVehicles(vehicles: List<Vehicle>) = vehicles.sortedByDescending { it.isPrimary }

    fun vehicleLabel(v: Vehicle) = "${v.brand} ${v.model} · ${v.plate}"
}

/**
 * Inscripción a un evento (POST /api/registrations): vehículo propio, acompañantes y aceptación del reglamento.
 * El evento y los vehículos se piden al backend; él vuelve a validar cupos, fechas, dueño del vehículo y duplicados.
 */
class RegistrationFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }

    private lateinit var binding: ActivityRegistrationFormBinding
    private val eventId by lazy { intent.getIntExtra(EXTRA_EVENT_ID, -1) }

    private var vehicles: List<Vehicle> = emptyList()
    private var vehicleIndex = 0
    private var companions = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)  // por si Android recreó la app directamente en esta pantalla
        binding = ActivityRegistrationFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnRetry.setOnClickListener { load() }
        binding.etVehicle.setOnItemClickListener { _, _, position, _ -> vehicleIndex = position }
        binding.etCompanions.setOnItemClickListener { _, _, position, _ -> companions = RegistrationForm.COMPANIONS.first + position }
        // Sin aceptar el reglamento no se puede confirmar.
        binding.cbRules.setOnCheckedChangeListener { _, checked -> binding.btnConfirm.isEnabled = checked }
        binding.btnConfirm.setOnClickListener { confirm() }
        load()
    }

    private fun load() {
        val memberId = SessionManager.id(this) ?: return LoginActivity.restart(this)
        lifecycleScope.launch {
            showState(loading = true)
            try {
                val (event, own) = coroutineScope {
                    val e = async { RetrofitClient.api.getEvent(eventId) }
                    val v = async { RetrofitClient.api.getVehicles(memberId) }
                    e.await() to v.await()
                }
                if (own.isEmpty()) {
                    // El garaje es una pestaña de MainActivity: se vuelve atrás para llegar a él.
                    MaterialAlertDialogBuilder(this@RegistrationFormActivity)
                        .setMessage(R.string.no_vehicles_to_register)
                        .setPositiveButton(android.R.string.ok, null)
                        .setOnDismissListener { finish() }
                        .show()
                    return@launch
                }
                show(event, RegistrationForm.sortVehicles(own))
                showState(content = true)
            } catch (e: Exception) {
                if (ApiError.isUnauthorized(e)) return@launch LoginActivity.restart(this@RegistrationFormActivity)
                binding.tvError.text = ApiError.message(e)
                showState(error = true)
            }
        }
    }

    private fun show(e: Event, sorted: List<Vehicle>) {
        binding.ivCategory.setImageResource(EventDetailActivity.categoryIcon(e.category))
        binding.tvWhen.text = "${Format.dateRange(e.date, e.endDate)} · ${Format.time(e.time)}"
        binding.tvName.text = e.name
        binding.tvLocation.text = e.location
        binding.tvPrice.text = Format.price(e.price) ?: getString(R.string.free)

        vehicles = sorted
        vehicleIndex = 0
        binding.etVehicle.setSimpleItems(sorted.map(RegistrationForm::vehicleLabel).toTypedArray())
        binding.etVehicle.setText(RegistrationForm.vehicleLabel(sorted[0]), false)  // false: no abrir ni filtrar la lista

        val labels = RegistrationForm.COMPANIONS.map { n ->
            if (n == 0) getString(R.string.no_companions) else resources.getQuantityString(R.plurals.companions_count, n, n)
        }
        binding.etCompanions.setSimpleItems(labels.toTypedArray())
        binding.etCompanions.setText(labels[companions - RegistrationForm.COMPANIONS.first], false)
    }

    private fun confirm() {
        val body = RegistrationRequest(eventId, vehicles[vehicleIndex].id, companions)
        lifecycleScope.launch {
            setSending(true)
            try {
                val registration = RetrofitClient.api.createRegistration(body)
                // Solo con el 201 del backend se confirma la inscripción; "atrás" desde ahí vuelve al detalle.
                startActivity(RegistrationSuccessActivity.intent(this@RegistrationFormActivity, registration))
                finish()
            } catch (e: Exception) {
                if (ApiError.isUnauthorized(e)) return@launch LoginActivity.restart(this@RegistrationFormActivity)
                // 409 (ya inscrito, sin cupos, el evento empezó): nunca se muestra la confirmación; se vuelve al detalle.
                val conflict = e is HttpException && e.code() == 409
                MaterialAlertDialogBuilder(this@RegistrationFormActivity)
                    .setMessage(ApiError.message(e))
                    .setPositiveButton(android.R.string.ok, null)
                    .setOnDismissListener { if (conflict) finish() }
                    .show()
            } finally {
                setSending(false)
            }
        }
    }

    private fun setSending(sending: Boolean) {
        binding.progress.isVisible = sending
        binding.btnConfirm.isEnabled = !sending && binding.cbRules.isChecked  // evita un doble toque
    }

    private fun showState(loading: Boolean = false, error: Boolean = false, content: Boolean = false) {
        binding.progress.isVisible = loading
        binding.errorGroup.isVisible = error
        binding.content.isVisible = content
        binding.btnConfirm.isVisible = content
    }
}
