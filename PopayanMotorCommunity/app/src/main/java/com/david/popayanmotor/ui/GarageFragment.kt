package com.david.popayanmotor.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.david.popayanmotor.R
import com.david.popayanmotor.data.ApiError
import com.david.popayanmotor.data.RetrofitClient
import com.david.popayanmotor.data.SessionManager
import com.david.popayanmotor.data.Vehicle
import com.david.popayanmotor.databinding.FragmentGarageBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Pestaña Garaje: vehículos del miembro (GET /api/vehicles devuelve solo los del token). */
class GarageFragment : Fragment() {

    private var _binding: FragmentGarageBinding? = null
    private val binding get() = _binding!!

    private val adapter = VehicleAdapter(onEdit = ::openForm, onDelete = ::confirmDelete)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGarageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rvVehicles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVehicles.adapter = adapter
        binding.btnAdd.setOnClickListener { openForm(null) }
        binding.btnAddBottom.setOnClickListener { openForm(null) }
        binding.btnRetry.setOnClickListener { loadVehicles() }
    }

    // Se recarga al volver del formulario, así la lista siempre refleja lo guardado.
    override fun onResume() {
        super.onResume()
        loadVehicles()
    }

    private fun openForm(vehicle: Vehicle?) {
        val intent = Intent(requireContext(), VehicleFormActivity::class.java)
        vehicle?.let { intent.putExtra(VehicleFormActivity.EXTRA_ID, it.id) }
        startActivity(intent)
    }

    private fun loadVehicles() {
        viewLifecycleOwner.lifecycleScope.launch {
            showState(loading = true)
            try {
                showVehicles(RetrofitClient.api.getVehicles(SessionManager.id(requireContext())!!))  // MainActivity garantiza el id
            } catch (e: Exception) {
                handleError(e) { binding.tvError.text = it; showState(error = true) }
            }
        }
    }

    private fun showVehicles(vehicles: List<Vehicle>) {
        // El principal primero; el backend los ordena por marca y modelo.
        adapter.submit(vehicles.sortedByDescending { it.isPrimary })
        binding.tvCount.text = resources.getQuantityString(R.plurals.vehicle_count, vehicles.size, vehicles.size)
        if (vehicles.isEmpty()) showState(empty = true) else showState(content = true)
    }

    private fun confirmDelete(vehicle: Vehicle) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.delete_vehicle_confirm, "${vehicle.brand} ${vehicle.model}"))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> delete(vehicle) }
            .show()
    }

    private fun delete(vehicle: Vehicle) {
        viewLifecycleOwner.lifecycleScope.launch {
            showState(loading = true)
            try {
                val response = RetrofitClient.api.deleteVehicle(vehicle.id)
                if (!response.isSuccessful) throw HttpException(response)  // ej. 409: tiene inscripciones
                Toast.makeText(requireContext(), R.string.vehicle_deleted, Toast.LENGTH_SHORT).show()
                loadVehicles()
            } catch (e: Exception) {
                // El vehículo no se borró: se avisa por qué y se vuelve a mostrar la lista.
                handleError(e) { message ->
                    MaterialAlertDialogBuilder(requireContext()).setMessage(message).setPositiveButton(android.R.string.ok, null).show()
                    loadVehicles()
                }
            }
        }
    }

    private fun handleError(e: Exception, show: (String) -> Unit) {
        if (ApiError.isUnauthorized(e)) {
            (requireActivity() as MainActivity).logout()  // token vencido → volver al login
        } else {
            show(ApiError.message(e))
        }
    }

    /** Muestra solo una de las vistas: carga, error, vacío o lista. */
    private fun showState(loading: Boolean = false, error: Boolean = false, empty: Boolean = false, content: Boolean = false) {
        binding.progress.isVisible = loading
        binding.errorGroup.isVisible = error
        binding.tvEmpty.isVisible = empty
        binding.rvVehicles.isVisible = content
        binding.btnAddBottom.isVisible = empty || content
        binding.tvCount.isVisible = empty || content
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
