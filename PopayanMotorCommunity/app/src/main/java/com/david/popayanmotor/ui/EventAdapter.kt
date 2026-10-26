package com.david.popayanmotor.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.david.popayanmotor.R
import com.david.popayanmotor.data.Event
import com.david.popayanmotor.databinding.ItemEventBinding

/** Pinta cada evento de la lista como una tarjeta (item_event.xml). onClick null = la tarjeta no se puede tocar. */
class EventAdapter(private val onClick: ((Event) -> Unit)? = null) : RecyclerView.Adapter<EventAdapter.Holder>() {

    private var events: List<Event> = emptyList()

    @SuppressLint("NotifyDataSetChanged")  // la lista se reemplaza completa en cada búsqueda
    fun submit(list: List<Event>) {
        events = list
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = events.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val e = events[position]
        val b = holder.binding
        val ctx = b.root.context

        b.tvDay.text = Format.day(e.date)
        b.tvMonth.text = Format.month(e.date)
        b.tvName.text = e.name
        b.tvCategory.text = e.category.uppercase()
        b.tvWhenWhere.text = "${Format.dateRange(e.date, e.endDate)} · ${Format.time(e.time)} · ${e.location}"
        // Cupos = inscripciones; los acompañantes no ocupan cupo (regla aprobada).
        b.tvCapacity.text = "${e.registeredCount}/${e.maxCapacity} cupos"
        b.tvPrice.text = Format.price(e.price) ?: ctx.getString(R.string.free)
        b.tvDistance.isVisible = e.distanceKm != null
        e.distanceKm?.let { b.tvDistance.text = Format.distance(it) }
        b.tvFinished.isVisible = e.finished
        b.root.alpha = if (e.finished) 0.6f else 1f
        b.root.setOnClickListener(onClick?.let { click -> { click(e) } })
    }
}
