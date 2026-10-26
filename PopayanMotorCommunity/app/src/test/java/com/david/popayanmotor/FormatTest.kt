package com.david.popayanmotor

import com.david.popayanmotor.data.ApiError
import com.david.popayanmotor.data.Registration
import com.david.popayanmotor.data.RegistrationEvent
import com.david.popayanmotor.ui.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTest {

    @Test
    fun fechas() {
        assertEquals("07", Format.day("2026-11-07"))
        assertEquals("NOV", Format.month("2026-11-07"))
        assertEquals("07 NOV", Format.dateRange("2026-11-07", null))
        assertEquals("31 DIC – 01 ENE", Format.dateRange("2026-12-31", "2027-01-01"))
        assertEquals("10:00", Format.time("10:00:00"))
    }

    @Test
    fun precioYDistancia() {
        assertNull(Format.price(0))
        assertEquals("$25.000", Format.price(25000))
        assertEquals("12 km", Format.distance(12.0))
        assertEquals("12.5 km", Format.distance(12.5))
    }

    @Test
    fun saludoSegunLaHora() {
        assertEquals("Buenos días, David", Format.greeting(5, "David López"))
        assertEquals("Buenos días, David", Format.greeting(11, " David "))
        assertEquals("Buenas tardes, David", Format.greeting(12, "David"))
        assertEquals("Buenas tardes, David", Format.greeting(18, "David"))
        assertEquals("Buenas noches, David", Format.greeting(19, "David"))
        assertEquals("Buenas noches, David", Format.greeting(0, "David"))
        assertEquals("Buenas noches", Format.greeting(4, ""))  // sesión sin nombre
    }

    @Test
    fun perfil() {
        assertEquals("AM", Format.initials("Andrés Muñoz"))
        assertEquals("DL", Format.initials("  david   lópez  garcía "))
        assertEquals("D", Format.initials("David"))
        assertEquals("marzo de 2023", Format.monthYear("2023-03-14T10:00:00"))
        assertEquals("diciembre de 2026", Format.monthYear("2026-12-01T00:00:00"))
        assertEquals("0", Format.km(0.0))
        assertEquals("240", Format.km(239.6))
        assertEquals("2.4K", Format.km(2400.0))
        assertEquals("1K", Format.km(1000.0))
    }

    @Test
    fun kmSoloDeRutasTerminadas() {
        fun reg(expires: String, km: Double?) = Registration(1, expires, RegistrationEvent(km))
        val now = "2026-10-26T12:00:00"
        val regs = listOf(
            reg("2026-10-20T00:01:00", 120.0),    // terminado → cuenta
            reg("2026-10-26T00:01:00", 30.5),     // terminó hoy a las 00:01 → cuenta
            reg("2026-10-20T00:01:00", null),     // terminado pero sin distancia (ej. un Meet) → 0
            reg("2026-11-07T00:01:00", 500.0),    // aún no termina → no cuenta
        )
        assertEquals(150.5, Format.kmInRoutes(regs, now), 0.001)
        assertEquals(0.0, Format.kmInRoutes(emptyList(), now), 0.001)
    }

    @Test
    fun errorDelBackend() {
        assertEquals("El evento ya alcanzó su capacidad máxima",
            ApiError.fromBody("""{"detail": "El evento ya alcanzó su capacidad máxima", "code": "EVENT_FULL"}"""))
        assertNull(ApiError.fromBody("Internal Server Error"))
        assertNull(ApiError.fromBody(null))
    }
}
