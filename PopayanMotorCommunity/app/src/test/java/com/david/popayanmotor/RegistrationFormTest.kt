package com.david.popayanmotor

import com.david.popayanmotor.data.RegistrationRequest
import com.david.popayanmotor.data.Vehicle
import com.david.popayanmotor.ui.RegistrationForm
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class RegistrationFormTest {

    private fun vehicle(id: Int, primary: Boolean) =
        Vehicle(id, "Mazda", "MX-5", 2020, "Deportivo", "PRB12$id", null, primary)

    @Test
    fun acompanantesComoElBackend() {
        // RegistrationIn: companions entre 0 y 10
        assertEquals(0, RegistrationForm.COMPANIONS.first)
        assertEquals(10, RegistrationForm.COMPANIONS.last)
        assertEquals(11, RegistrationForm.COMPANIONS.count())
    }

    @Test
    fun principalPrimeroYEtiqueta() {
        val sorted = RegistrationForm.sortVehicles(listOf(vehicle(1, false), vehicle(2, true), vehicle(3, false)))
        assertEquals(listOf(2, 1, 3), sorted.map { it.id })  // los demás conservan el orden
        assertEquals("Mazda MX-5 · PRB121", RegistrationForm.vehicleLabel(vehicle(1, false)))
    }

    @Test
    fun cuerpoQueRecibeElBackend() {
        assertEquals("""{"event_id":4,"vehicle_id":7,"companions":2}""", Gson().toJson(RegistrationRequest(4, 7, 2)))
    }
}
