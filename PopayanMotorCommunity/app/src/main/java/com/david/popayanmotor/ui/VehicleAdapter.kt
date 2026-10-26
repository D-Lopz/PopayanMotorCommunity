package com.david.popayanmotor.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.david.popayanmotor.data.Vehicle
import com.david.popayanmotor.databinding.ItemVehicleBinding

/** Tarjetas del garaje (item_vehicle.xml). Tocar la tarjeta o "Editar" abre el formulario. */
class VehicleAdapter(
    private val onEdit: (Vehicle) -> Unit,
    private val onDelete: (Vehicle) -> Unit
) : RecyclerView.Adapter<VehicleAdapter.Holder>() {

    private var vehicles: List<Vehicle> = emptyList()

    @SuppressLint("NotifyDataSetChanged")  // la lista se reemplaza completa en cada carga
    fun submit(list: List<Vehicle>) {
        vehicles = list
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemVehicleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemVehicleBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = vehicles.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val v = vehicles[position]
        val b = holder.binding
        b.tvMeta.text = "${v.year} · ${v.type.uppercase()}"
        b.tvName.text = "${v.brand} ${v.model}"
        b.tvDetail.text = listOfNotNull(v.plate, v.description?.takeIf { it.isNotBlank() }).joinToString(" · ")
        b.tvPrimary.isVisible = v.isPrimary
        b.root.setOnClickListener { onEdit(v) }
        b.btnEdit.setOnClickListener { onEdit(v) }
        b.btnDelete.setOnClickListener { onDelete(v) }
    }
}
