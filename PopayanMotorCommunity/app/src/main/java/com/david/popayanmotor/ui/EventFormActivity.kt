package com.david.popayanmotor.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.david.popayanmotor.R
import com.david.popayanmotor.data.ApiError
import com.david.popayanmotor.data.Event
import com.david.popayanmotor.data.EventRequest
import com.david.popayanmotor.data.RetrofitClient
import com.david.popayanmotor.data.SessionManager
import com.david.popayanmotor.databinding.ActivityEventFormBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Reglas del formulario de evento (sin Android, para probarlas con JUnit). Son las mismas de EventIn en el backend. */
object EventForm {

    /** "2026-11-07" + "16:00" es futuro si es posterior a now ("yyyy-MM-ddTHH:mm:ss", hora Colombia). */
    fun isFuture(date: String, time: String, now: String) = "${date}T$time" > now.take(16)

    /** La fecha de fin es opcional; si existe no puede ser anterior al inicio. */
    fun isValidEndDate(date: String, endDate: String?) = endDate == null || endDate >= date

    fun capacity(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 1..100_000 }

    /** Vacío = 0 (gratis). */
    fun price(text: String): Int? = text.trim().ifEmpty { "0" }.toIntOrNull()?.takeIf { it in 0..100_000_000 }

    /** Vacío = sin distancia (válido). Acepta coma o punto decimal. */
    fun isValidDistance(text: String) = text.isBlank() || distance(text) != null

    fun distance(text: String): Double? =
        text.trim().replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 && it <= 10_000 }
}

/** Crear (sin EXTRA_ID) o editar/eliminar (con EXTRA_ID) un evento. Solo se abre desde la UI del admin. */
class EventFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "event_id"
        private const val STATE_ORIGINAL_START = "original_start"
    }

    private lateinit var binding: ActivityEventFormBinding
    private val eventId by lazy { intent.getIntExtra(EXTRA_ID, -1).takeIf { it != -1 } }
    private val categories by lazy { resources.getStringArray(R.array.event_categories) }

    // Al editar, la regla de "fecha futura" solo aplica si el admin cambia la fecha u hora (igual que el backend).
    private var originalStart: String? = null

    // Los selectores de Material trabajan con milisegundos en UTC.
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)  // por si Android recreó la app directamente en esta pantalla
        binding = ActivityEventFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setTitle(if (eventId == null) R.string.create_event else R.string.edit_event)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.etCategory.setSimpleItems(categories)
        binding.etDate.setOnClickListener { pickDate(binding.etDate) }
        binding.etEndDate.setOnClickListener { pickDate(binding.etEndDate) }
        binding.tilEndDate.setEndIconOnClickListener { binding.etEndDate.text = null }
        binding.etTime.setOnClickListener { pickTime() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnDelete.isVisible = eventId != null
        binding.btnDelete.setOnClickListener { confirmDelete() }

        originalStart = savedInstanceState?.getString(STATE_ORIGINAL_START)
        // Al rotar, los campos se restauran solos; solo se cargan del backend la primera vez.
        if (savedInstanceState == null) eventId?.let { load(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_ORIGINAL_START, originalStart)
    }

    private fun load(id: Int) {
        lifecycleScope.launch {
            setLoading(true)
            binding.form.isVisible = false
            try {
                fill(RetrofitClient.api.getEvent(id))
                binding.form.isVisible = true
            } catch (e: Exception) {
                fail(e)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun fill(e: Event) {
        binding.etName.setText(e.name)
        binding.etCategory.setText(e.category, false)  // false: no abrir ni filtrar la lista
        binding.etDescription.setText(e.description.orEmpty())
        binding.etDate.setText(e.date)
        binding.etTime.setText(Format.time(e.time))
        binding.etEndDate.setText(e.endDate.orEmpty())
        binding.etLocation.setText(e.location)
        binding.etCapacity.setText(e.maxCapacity.toString())
        binding.etPrice.setText(e.price.toString())
        binding.etDistance.setText(e.distanceKm?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty())
        originalStart = "${e.date}T${Format.time(e.time)}"
    }

    private fun pickDate(target: TextInputEditText) {
        val current = target.text.toString().ifEmpty { binding.etDate.text.toString() }
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(if (current.isEmpty()) MaterialDatePicker.todayInUtcMilliseconds() else dayFormat.parse(current)!!.time)
            .setCalendarConstraints(CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build())
            .build()
        picker.addOnPositiveButtonClickListener { millis -> target.setText(dayFormat.format(Date(millis))) }
        picker.show(supportFragmentManager, "date")
    }

    private fun pickTime() {
        val current = binding.etTime.text.toString()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(current.take(2).toIntOrNull() ?: 16)
            .setMinute(current.drop(3).toIntOrNull() ?: 0)
            .build()
        picker.addOnPositiveButtonClickListener {
            binding.etTime.setText(String.format(Locale.US, "%02d:%02d", picker.hour, picker.minute))
        }
        picker.show(supportFragmentManager, "time")
    }

    private fun save() {
        val name = binding.etName.text.toString().trim()
        val category = binding.etCategory.text.toString()
        val description = binding.etDescription.text.toString().trim()
        val date = binding.etDate.text.toString()
        val time = binding.etTime.text.toString()
        val endDate = binding.etEndDate.text.toString().ifEmpty { null }
        val location = binding.etLocation.text.toString().trim()
        val capacity = EventForm.capacity(binding.etCapacity.text.toString())
        val price = EventForm.price(binding.etPrice.text.toString())
        val distanceText = binding.etDistance.text.toString()

        val startChanged = originalStart != "${date}T$time"
        binding.tilName.error = if (name.length in 3..150) null else getString(R.string.error_event_name)
        binding.tilCategory.error = if (category in categories) null else getString(R.string.error_category)
        binding.tilDate.error = when {
            date.isEmpty() -> getString(R.string.error_required)
            time.isNotEmpty() && startChanged && !EventForm.isFuture(date, time, Format.nowColombia()) ->
                getString(R.string.error_future)
            else -> null
        }
        binding.tilTime.error = if (time.isEmpty()) getString(R.string.error_required) else null
        binding.tilEndDate.error =
            if (date.isEmpty() || EventForm.isValidEndDate(date, endDate)) null else getString(R.string.error_end_date)
        binding.tilLocation.error = if (location.length in 2..200) null else getString(R.string.error_location)
        binding.tilCapacity.error = if (capacity != null) null else getString(R.string.error_capacity)
        binding.tilPrice.error = if (price != null) null else getString(R.string.error_price)
        binding.tilDistance.error = if (EventForm.isValidDistance(distanceText)) null else getString(R.string.error_distance)
        val tils = listOf(
            binding.tilName, binding.tilCategory, binding.tilDate, binding.tilTime, binding.tilEndDate,
            binding.tilLocation, binding.tilCapacity, binding.tilPrice, binding.tilDistance
        )
        if (capacity == null || price == null || tils.any { it.error != null }) return

        val body = EventRequest(
            name, description.ifEmpty { null }, date, endDate, time, location,
            capacity, category, price, EventForm.distance(distanceText)
        )
        lifecycleScope.launch {
            setLoading(true)
            try {
                val id = eventId
                if (id == null) RetrofitClient.api.createEvent(body) else RetrofitClient.api.updateEvent(id, body)
                Toast.makeText(this@EventFormActivity, R.string.event_saved, Toast.LENGTH_SHORT).show()
                finish()  // la lista de eventos recarga en onResume
            } catch (e: Exception) {
                fail(e)  // ej: "La capacidad no puede ser menor a los 3 inscritos actuales"
            } finally {
                setLoading(false)
            }
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.delete_event_confirm, binding.etName.text.toString()))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> delete() }
            .show()
    }

    private fun delete() {
        val id = eventId ?: return
        lifecycleScope.launch {
            setLoading(true)
            try {
                val response = RetrofitClient.api.deleteEvent(id)
                if (!response.isSuccessful) throw HttpException(response)  // ej. 409: ya tiene inscripciones
                Toast.makeText(this@EventFormActivity, R.string.event_deleted, Toast.LENGTH_SHORT).show()
                finish()
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
        binding.form.isVisible = true
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.isVisible = loading
        binding.btnSave.isEnabled = !loading
        binding.btnDelete.isEnabled = !loading
        if (loading) binding.tvError.isVisible = false
    }
}
