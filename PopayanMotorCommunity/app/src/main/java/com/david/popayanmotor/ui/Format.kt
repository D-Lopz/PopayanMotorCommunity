package com.david.popayanmotor.ui

import com.david.popayanmotor.data.Registration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Formatos para mostrar en pantalla los valores que llegan del backend como texto. */
object Format {

    private val MONTHS = listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")

    /** "2026-11-07" → "07" */
    fun day(date: String) = date.substring(8, 10)

    /** "2026-11-07" → "NOV" */
    fun month(date: String) = MONTHS[date.substring(5, 7).toInt() - 1]

    /** "2026-11-07" + "2026-11-08" → "07 NOV – 08 NOV"; sin fin → "07 NOV" */
    fun dateRange(date: String, endDate: String?): String {
        val start = "${day(date)} ${month(date)}"
        return if (endDate == null || endDate == date) start else "$start – ${day(endDate)} ${month(endDate)}"
    }

    /** "10:00:00" → "10:00" */
    fun time(time: String) = time.take(5)

    /** 25000 → "$25.000"; 0 → null (la pantalla muestra "Gratis") */
    fun price(cop: Int): String? =
        if (cop == 0) null else "$" + String.format(Locale.GERMANY, "%,d", cop)  // GERMANY: separador de miles "."

    /** 12.0 → "12 km"; 12.5 → "12.5 km" */
    fun distance(km: Double) = if (km % 1.0 == 0.0) "${km.toInt()} km" else "$km km"

    private val MONTH_NAMES = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )

    /** "Andrés Muñoz" → "AM"; "David" → "D" (avatar sin foto). */
    fun initials(name: String) =
        name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1).uppercase() }

    /** "2023-03-14T10:00:00" → "marzo de 2023" */
    fun monthYear(dateTime: String) = "${MONTH_NAMES[dateTime.substring(5, 7).toInt() - 1]} de ${dateTime.take(4)}"

    /** 240.0 → "240"; 2400.0 → "2.4K" (como el diseño del perfil) */
    fun km(km: Double): String =
        if (km < 1000) Math.round(km).toString()
        else String.format(Locale.US, "%.1fK", km / 1000).replace(".0K", "K")

    /**
     * Km en rutas: suma la distancia de los eventos YA TERMINADOS a los que el miembro se inscribió.
     * Es una estimación: inscribirse no confirma que asistió ni que recorrió esos km.
     * now y codeExpiresAt van en "yyyy-MM-ddTHH:mm:ss" (hora Colombia): como texto se comparan igual que como fecha.
     */
    fun kmInRoutes(registrations: List<Registration>, now: String): Double =
        registrations.filter { it.codeExpiresAt <= now }.sumOf { it.event.distanceKm ?: 0.0 }

    /** Hora actual de Colombia como "yyyy-MM-ddTHH:mm:ss", el mismo formato de fechas del backend. */
    fun nowColombia(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("America/Bogota") }
            .format(Date())

    /** (9, "David López") → "Buenos días, David". Mañana 5–11 h, tarde 12–18 h, noche el resto. */
    fun greeting(hour: Int, fullName: String): String {
        val text = when (hour) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
        val firstName = fullName.trim().substringBefore(" ")
        return if (firstName.isEmpty()) text else "$text, $firstName"
    }
}
