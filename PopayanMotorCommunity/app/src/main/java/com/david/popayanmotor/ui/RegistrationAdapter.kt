package com.david.popayanmotor.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.david.popayanmotor.R
import com.david.popayanmotor.data.Registration
import com.david.popayanmotor.databinding.ItemRegistrationBinding

/** Tarjetas de "Mis inscripciones" (item_registration.xml). Solo las próximas muestran "Cancelar". */
class RegistrationAdapter(private val onCancel: (Registration) -> Unit) : RecyclerView.Adapter<RegistrationAdapter.Holder>() {

    private var registrations: List<Registration> = emptyList()
    private var now = ""

    @SuppressLint("NotifyDataSetChanged")  // la lista se reemplaza completa en cada carga
    fun submit(list: List<Registration>, now: String) {
        registrations = list
        this.now = now
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemRegistrationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemRegistrationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = registrations.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val r = registrations[position]
        val e = r.event
        val b = holder.binding
        val upcoming = !Format.hasStarted(e.date, e.time, now)

        b.tvCode.text = r.code
        b.tvStatus.setText(if (upcoming) R.string.upcoming else R.string.past)
        b.tvName.text = e.name
        b.tvWhenWhere.text = "${Format.dateRange(e.date, e.endDate)} · ${Format.time(e.time)} · ${e.location}"
        b.tvVehicle.isVisible = r.vehicle != null
        r.vehicle?.let { b.tvVehicle.text = "${it.brand} ${it.model} · ${it.plate}" }
        b.divider.isVisible = upcoming
        b.btnCancel.isVisible = upcoming
        b.btnCancel.setOnClickListener { onCancel(r) }
        b.root.alpha = if (upcoming) 1f else 0.6f
    }
}
