package com.david.popayanmotor

import com.david.popayanmotor.ui.VehicleForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleFormTest {

    @Test
    fun anio() {
        assertEquals(2019, VehicleForm.year(" 2019 ", 2026))
        assertEquals(1900, VehicleForm.year("1900", 2026))
        assertEquals(2027, VehicleForm.year("2027", 2026))  // modelo del año siguiente
        assertNull(VehicleForm.year("2028", 2026))
        assertNull(VehicleForm.year("1899", 2026))
        assertNull(VehicleForm.year("", 2026))
    }

    @Test
    fun placa() {
        assertEquals("ABC123", VehicleForm.plate(" abc-123 "))
        assertEquals("KLT482", VehicleForm.plate("KLT 482"))
        assertNull(VehicleForm.plate("AB1"))  // muy corta
        assertNull(VehicleForm.plate("ABC12345"))  // muy larga
        assertNull(VehicleForm.plate("ABC.12"))
    }

    private val catalog = listOf("Mazda", "Mini", "Mercedes-Benz")

    @Test
    fun marca() {
        assertEquals("Mazda", VehicleForm.brand("Mazda", "ignorado", catalog))
        assertEquals("Great Wall", VehicleForm.brand("Otra", "  Great   Wall ", catalog))
        assertNull(VehicleForm.brand("", "", catalog))             // sin elegir
        assertNull(VehicleForm.brand("Otra", "", catalog))         // «Otra» sin escribir nada
        assertNull(VehicleForm.brand("Otra", "otra", catalog))     // «Otra» no es una marca
        assertNull(VehicleForm.brand("Otra", "X", catalog))
        assertNull(VehicleForm.brand("Otra", "123", catalog))
        assertNull(VehicleForm.brand("Otra", "<script>", catalog))
        assertNull(VehicleForm.brand("Toyota", "", catalog))       // no está en el catálogo descargado
    }

    @Test
    fun marcaAlEditar() {
        assertEquals("Mazda" to "", VehicleForm.brandChoice("Mazda", catalog))
        assertEquals("Mini" to "", VehicleForm.brandChoice("mini", catalog))
        assertEquals("Otra" to "Great Wall", VehicleForm.brandChoice("Great Wall", catalog))
    }
}
