package com.david.popayanmotor.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.david.popayanmotor.R
import com.david.popayanmotor.data.Registration
import com.david.popayanmotor.databinding.ActivityRegistrationSuccessBinding

/**
 * "Inscripción confirmada" con el QR. Solo se abre con la inscripción que devolvió el backend (201):
 * todo lo que muestra viene de esa respuesta, nada se genera en la app salvo la imagen del QR.
 */
class RegistrationSuccessActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_CODE = "code"
        private const val EXTRA_EVENT = "event_name"
        private const val EXTRA_DATE = "date"
        private const val EXTRA_END_DATE = "end_date"
        private const val EXTRA_TIME = "time"
        private const val EXTRA_LOCATION = "location"

        fun intent(context: Context, r: Registration): Intent = Intent(context, RegistrationSuccessActivity::class.java)
            .putExtra(EXTRA_CODE, r.code)
            .putExtra(EXTRA_EVENT, r.event.name)
            .putExtra(EXTRA_DATE, r.event.date)
            .putExtra(EXTRA_END_DATE, r.event.endDate)
            .putExtra(EXTRA_TIME, r.event.time)
            .putExtra(EXTRA_LOCATION, r.event.location)
    }

    private lateinit var binding: ActivityRegistrationSuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val code = intent.getStringExtra(EXTRA_CODE) ?: return finish()
        val date = intent.getStringExtra(EXTRA_DATE)!!
        binding.tvReserved.text = getString(R.string.spot_reserved, intent.getStringExtra(EXTRA_EVENT))
        binding.tvCode.text = code
        binding.tvWhen.text = getString(
            R.string.date_time, Format.dateRange(date, intent.getStringExtra(EXTRA_END_DATE)), Format.time(intent.getStringExtra(EXTRA_TIME)!!)
        )
        binding.tvLocation.text = intent.getStringExtra(EXTRA_LOCATION)

        // Un píxel de la imagen por píxel de pantalla (sin escalar): bordes nítidos para el lector.
        val size = resources.getDimensionPixelSize(R.dimen.qr_large) - binding.ivQr.paddingLeft - binding.ivQr.paddingRight
        binding.ivQr.setImageBitmap(
            Qr.bitmap(code, size, ContextCompat.getColor(this, R.color.bg), ContextCompat.getColor(this, R.color.text_primary))
        )
        binding.ivQr.contentDescription = getString(R.string.qr_description, code)

        binding.btnViewRegistration.setOnClickListener {
            startActivity(Intent(this, RegistrationsActivity::class.java))
            finish()  // "atrás" desde Mis inscripciones vuelve al detalle, que ya dice "Ver mi inscripción"
        }
        binding.btnHome.setOnClickListener {
            // Inicio nuevo y sin pantallas atrás: MainActivity abre la pestaña Inicio.
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
    }
}
