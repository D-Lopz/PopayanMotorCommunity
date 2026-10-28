package com.david.popayanmotor.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.david.popayanmotor.R
import com.david.popayanmotor.data.ApiError
import com.david.popayanmotor.data.Event
import com.david.popayanmotor.data.RetrofitClient
import com.david.popayanmotor.data.SessionManager
import com.david.popayanmotor.databinding.ActivityEventDetailBinding
import com.david.popayanmotor.databinding.ItemDetailRowBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Detalle de un evento (GET /api/events/{id}) para los dos roles.
 * Miembro: botón Inscribirme. Admin: lápiz para editar (el backend sigue rechazando con 403 a cualquier otro).
 */
class EventDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "event_id"

        /** Ícono según la categoría (los nombres son los del ENUM EventCategory del backend). También lo usa la inscripción. */
        fun categoryIcon(category: String) = when (category) {
            "Meet", "Reunión" -> R.drawable.ic_group
            "Exhibición" -> R.drawable.ic_garage
            "Ruta" -> R.drawable.ic_route
            "Competencia" -> R.drawable.ic_flag
            "Evento de sonido" -> R.drawable.ic_sound
            else -> R.drawable.ic_event
        }
    }

    private lateinit var binding: ActivityEventDetailBinding
    private val eventId by lazy { intent.getIntExtra(EXTRA_ID, -1) }
    private val isAdmin by lazy { SessionManager.isAdmin(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)  // por si Android recreó la app directamente en esta pantalla
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.menu.findItem(R.id.action_edit).isVisible = isAdmin
        binding.toolbar.setOnMenuItemClickListener {
            startActivity(Intent(this, EventFormActivity::class.java).putExtra(EventFormActivity.EXTRA_ID, eventId))
            true
        }
        binding.btnRetry.setOnClickListener { load() }
    }

    // Se recarga al volver del formulario del admin y después de inscribirse (cupos al día).
    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            showState(loading = true)
            try {
                // Miembro: el evento y su inscripción a él en paralelo. El estado sale del backend,
                // no de una variable local; el 409 ALREADY_REGISTERED sigue protegiendo los casos de carrera.
                val (event, registered) = coroutineScope {
                    val e = async { RetrofitClient.api.getEvent(eventId) }
                    val r = if (isAdmin) null else async {
                        RetrofitClient.api.getRegistrations(SessionManager.id(this@EventDetailActivity)!!, eventId)
                    }
                    e.await() to (r?.await()?.isNotEmpty() == true)
                }
                show(event, registered)
                showState(content = true)
            } catch (e: Exception) {
                if (ApiError.isUnauthorized(e)) return@launch LoginActivity.restart(this@EventDetailActivity)
                binding.tvError.text = ApiError.message(e)  // ej. 404 "El evento no existe" si el admin lo eliminó
                showState(error = true)
            }
        }
    }

    private fun show(e: Event, registered: Boolean) {
        binding.ivCategory.setImageResource(categoryIcon(e.category))
        binding.tvCategory.text = e.category
        binding.tvDate.text = Format.longDate(e.date, e.endDate)
        binding.tvName.text = e.name

        val now = Format.nowColombia()
        val status = Format.eventStatus(e.finished, Format.hasStarted(e.date, e.time, now), e.registeredCount, e.maxCapacity)
        binding.tvStatus.setText(
            when (status) {
                Format.EventStatus.OPEN -> R.string.status_open
                Format.EventStatus.FULL -> R.string.status_full
                Format.EventStatus.LIVE -> R.string.status_live
                Format.EventStatus.FINISHED -> R.string.finished
            }
        )
        // Rojo solo para "Cupos abiertos"; los demás estados van en neutro.
        binding.tvStatus.setBackgroundResource(if (status == Format.EventStatus.OPEN) R.drawable.bg_status_chip else R.drawable.bg_icon_box)

        row(binding.rowTime, R.drawable.ic_time, R.string.time, Format.time(e.time))
        row(binding.rowPlace, R.drawable.ic_place, R.string.meeting_point, e.location)
        val left = (e.maxCapacity - e.registeredCount).coerceAtLeast(0)
        row(binding.rowCapacity, R.drawable.ic_group, R.string.availability,
            resources.getQuantityString(R.plurals.spots_left, left, left, e.maxCapacity))
        binding.rowDistance.root.isVisible = e.distanceKm != null
        e.distanceKm?.let { row(binding.rowDistance, R.drawable.ic_distance, R.string.distance, Format.distance(it)) }

        val description = e.description?.takeIf { it.isNotBlank() }
        binding.aboutGroup.isVisible = description != null
        binding.tvDescription.text = description

        binding.tvPrice.text = Format.price(e.price) ?: getString(R.string.free)
        // Inscribirse: solo miembros y solo con cupos antes de que empiece (el backend valida lo mismo).
        binding.btnRegister.isVisible = !isAdmin
        val action = Format.registerAction(registered, status)
        binding.btnRegister.isEnabled = action != Format.RegisterAction.UNAVAILABLE
        binding.btnRegister.text = when (action) {
            Format.RegisterAction.REGISTER -> getString(R.string.register_event)
            Format.RegisterAction.VIEW_REGISTRATION -> getString(R.string.already_registered)
            // Si no se puede, el botón deshabilitado dice por qué ("Agotado", "En curso", "Terminado").
            Format.RegisterAction.UNAVAILABLE -> binding.tvStatus.text
        }
        binding.btnRegister.setOnClickListener {
            if (action == Format.RegisterAction.VIEW_REGISTRATION) {
                startActivity(Intent(this, RegistrationsActivity::class.java))
            } else {
                startActivity(Intent(this, RegistrationFormActivity::class.java)
                    .putExtra(RegistrationFormActivity.EXTRA_EVENT_ID, e.id))
            }
        }
    }

    private fun row(row: ItemDetailRowBinding, icon: Int, label: Int, value: String) {
        row.ivIcon.setImageResource(icon)
        row.tvLabel.setText(label)
        row.tvValue.text = value
    }

    private fun showState(loading: Boolean = false, error: Boolean = false, content: Boolean = false) {
        binding.progress.isVisible = loading
        binding.errorGroup.isVisible = error
        binding.content.isVisible = content
        binding.bottomBar.isVisible = content
    }
}
