package com.david.popayanmotor.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.david.popayanmotor.R
import com.david.popayanmotor.data.ApiError
import com.david.popayanmotor.data.RetrofitClient
import com.david.popayanmotor.data.SessionManager
import com.david.popayanmotor.data.Vehicle
import com.david.popayanmotor.data.VehicleRequest
import com.david.popayanmotor.databinding.ActivityVehicleFormBinding
import kotlinx.coroutines.launch
import java.util.Calendar

/** Reglas del formulario de vehículo (sin Android, para probarlas con JUnit). Son las mismas del backend. */
object VehicleForm {

    private val PLATE_RE = Regex("^[A-Z0-9]{5,7}$")

    fun maxYear(currentYear: Int) = currentYear + 1

    /** "1999" → 1999; null si no es número o está fuera de 1900..año actual+1. */
    fun year(text: String, currentYear: Int): Int? =
        text.trim().toIntOrNull()?.takeIf { it in 1900..maxYear(currentYear) }

    /** " abc-123 " → "ABC123"; null si no tiene 5 a 7 letras o números. */
    fun plate(text: String): String? =
        text.trim().uppercase().replace(" ", "").replace("-", "").takeIf { PLATE_RE.matches(it) }
}

/** Crear (sin EXTRA_ID) o editar (con EXTRA_ID) un vehículo del garaje. */
class VehicleFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "vehicle_id"
    }

    private lateinit var binding: ActivityVehicleFormBinding
    private val vehicleId by lazy { intent.getIntExtra(EXTRA_ID, -1).takeIf { it != -1 } }
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)  // por si Android recreó la app directamente en esta pantalla
        binding = ActivityVehicleFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setTitle(if (vehicleId == null) R.string.add_vehicle else R.string.edit_vehicle)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.etType.setSimpleItems(resources.getStringArray(R.array.vehicle_types))
        binding.btnSave.setOnClickListener { save() }

        // Al rotar, los campos se restauran solos; solo se cargan del backend la primera vez.
        if (savedInstanceState == null) vehicleId?.let { load(it) }
    }

    /** Consulta el detalle (GET /api/vehicles/{id}); el backend responde 403 si no es tuyo. */
    private fun load(id: Int) {
        lifecycleScope.launch {
            setLoading(true)
            binding.form.isVisible = false
            try {
                fill(RetrofitClient.api.getVehicle(id))
                binding.form.isVisible = true
            } catch (e: Exception) {
                fail(e)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun fill(v: Vehicle) {
        binding.etBrand.setText(v.brand)
        binding.etModel.setText(v.model)
        binding.etYear.setText(v.year.toString())
        binding.etPlate.setText(v.plate)
        binding.etType.setText(v.type, false)  // false: no abrir ni filtrar la lista al escribirlo
        binding.etDescription.setText(v.description.orEmpty())
        binding.swPrimary.isChecked = v.isPrimary
        // El backend ignora is_primary=false al editar (siempre debe quedar un principal).
        if (v.isPrimary) {
            binding.swPrimary.isEnabled = false
            binding.tvPrimaryHelp.setText(R.string.primary_locked)
        }
    }

    private fun save() {
        val brand = binding.etBrand.text.toString().trim()
        val model = binding.etModel.text.toString().trim()
        val year = VehicleForm.year(binding.etYear.text.toString(), currentYear)
        val plate = VehicleForm.plate(binding.etPlate.text.toString())
        val type = binding.etType.text.toString()
        val description = binding.etDescription.text.toString().trim()

        binding.tilBrand.error = if (brand.isNotEmpty()) null else getString(R.string.error_required)
        binding.tilModel.error = if (model.isNotEmpty()) null else getString(R.string.error_required)
        binding.tilYear.error = if (year != null) null else getString(R.string.error_year, VehicleForm.maxYear(currentYear))
        binding.tilPlate.error = if (plate != null) null else getString(R.string.error_plate)
        binding.tilType.error =
            if (type in resources.getStringArray(R.array.vehicle_types)) null else getString(R.string.error_type)
        if (year == null || plate == null ||
            listOf(binding.tilBrand, binding.tilModel, binding.tilType).any { it.error != null }) return

        val body = VehicleRequest(brand, model, year, type, plate, description.ifEmpty { null }, binding.swPrimary.isChecked)
        lifecycleScope.launch {
            setLoading(true)
            try {
                val id = vehicleId
                if (id == null) RetrofitClient.api.createVehicle(body) else RetrofitClient.api.updateVehicle(id, body)
                Toast.makeText(this@VehicleFormActivity, R.string.vehicle_saved, Toast.LENGTH_SHORT).show()
                finish()  // el garaje recarga la lista en onResume
            } catch (e: Exception) {
                fail(e)  // ej: "Ya existe un vehículo con esa placa"
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
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.isVisible = loading
        binding.btnSave.isEnabled = !loading
        if (loading) binding.tvError.isVisible = false
    }
}
