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
}
