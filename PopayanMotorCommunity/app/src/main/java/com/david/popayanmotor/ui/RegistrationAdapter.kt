package com.david.popayanmotor.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.david.popayanmotor.R
import com.david.popayanmotor.data.Registration
import com.david.popayanmotor.databinding.ItemRegistrationBinding

/** Tarjetas de "Mis inscripciones" (item_registration.xml): QR + código. Solo las próximas muestran "Cancelar". */
class RegistrationAdapter(private val onCancel: (Registration) -> Unit) : RecyclerView.Adapter<RegistrationAdapter.Holder>() {

    private var registrations: List<Registration> = emptyList()
    private var now = ""
    private val qrCache = HashMap<String, Bitmap>()  // el QR de cada código se genera una sola vez

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
        val ctx = b.root.context
        b.ivQr.setImageBitmap(qrCache.getOrPut(r.code) {
            val size = ctx.resources.getDimensionPixelSize(R.dimen.qr_small) - b.ivQr.paddingLeft - b.ivQr.paddingRight
            Qr.bitmap(r.code, size, ContextCompat.getColor(ctx, R.color.bg), ContextCompat.getColor(ctx, R.color.text_primary))
        })
        b.ivQr.contentDescription = ctx.getString(R.string.qr_description, r.code)
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
