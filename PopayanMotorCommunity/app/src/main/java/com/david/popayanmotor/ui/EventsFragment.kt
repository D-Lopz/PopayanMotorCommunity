package com.david.popayanmotor.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.david.popayanmotor.R
import com.david.popayanmotor.data.ApiError
import com.david.popayanmotor.data.Event
import com.david.popayanmotor.data.RetrofitClient
import com.david.popayanmotor.data.SessionManager
import com.david.popayanmotor.databinding.FragmentEventsBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Lista de eventos con búsqueda y filtro por categoría (GET /api/events?category=&search=).
 * Con home=true es la pestaña Inicio: saludo + próximos eventos, sin buscador ni filtros.
 */
class EventsFragment : Fragment() {

    companion object {
        private const val ARG_HOME = "home"

        fun home() = EventsFragment().apply { arguments = Bundle().apply { putBoolean(ARG_HOME, true) } }
    }

    private val isHome get() = arguments?.getBoolean(ARG_HOME) == true

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!

    private val isAdmin get() = SessionManager.isAdmin(requireContext())

    // Solo el admin puede tocar una tarjeta para editarla; el backend igual rechaza (403) a cualquier otro.
    private val adapter by lazy { EventAdapter(if (isAdmin) ::openForm else null) }
    private var category: String? = null  // null = "Todos"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter = adapter
        binding.btnRetry.setOnClickListener { loadEvents() }

        if (isHome) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)  // hora local del celular
            binding.tvGreeting.text = Format.greeting(hour, SessionManager.name(requireContext()))
            binding.tvGreeting.isVisible = true
            binding.tvTitle.setText(R.string.upcoming_events)
            binding.tvTitle.textSize = 14f  // etiqueta pequeña en rojo, como "2 VEHÍCULOS" del diseño
            binding.tvTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent))
            binding.tilSearch.isVisible = false
            binding.chipScroll.isVisible = false
            binding.tvEmpty.setText(R.string.upcoming_empty)
            return
        }

        binding.fabAdd.isVisible = isAdmin
        binding.fabAdd.setOnClickListener { openForm(null) }
        setupCategoryChips()

        // Buscar al presionar la lupa del teclado
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                loadEvents()
                true
            } else false
        }
    }

    // Se recarga al volver del formulario de evento, así la lista refleja lo creado/editado/eliminado.
    override fun onResume() {
        super.onResume()
        loadEvents()
    }

    private fun openForm(event: Event?) {
        val intent = Intent(requireContext(), EventFormActivity::class.java)
        event?.let { intent.putExtra(EventFormActivity.EXTRA_ID, it.id) }
        startActivity(intent)
    }

    private fun setupCategoryChips() {
        val names = listOf(getString(R.string.all)) + resources.getStringArray(R.array.event_categories)
        names.forEachIndexed { index, name ->
            val chip = Chip(requireContext()).apply {
                text = name
                isCheckable = true
                isChecked = index == 0
                setOnClickListener {
                    category = if (index == 0) null else name
                    loadEvents()
                }
            }
            binding.chipGroup.addView(chip)
        }
    }

    private fun loadEvents() {
        val search = binding.etSearch.text.toString().trim().ifEmpty { null }
        viewLifecycleOwner.lifecycleScope.launch {
            showState(loading = true)
            try {
                val events = RetrofitClient.api.getEvents(category, search)
                showEvents(if (isHome) events.filterNot { it.finished } else events)
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

    private fun showEvents(events: List<Event>) {
        adapter.submit(events)
        if (events.isEmpty()) showState(empty = true) else showState(content = true)
    }

    /** Muestra solo una de las cuatro vistas: carga, error, vacío o lista. */
    private fun showState(loading: Boolean = false, error: Boolean = false, empty: Boolean = false, content: Boolean = false) {
        binding.progress.isVisible = loading
        binding.errorGroup.isVisible = error
        binding.tvEmpty.isVisible = empty
        binding.rvEvents.isVisible = content
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // el Fragment vive más que su vista; se suelta para no filtrar memoria
    }
}
