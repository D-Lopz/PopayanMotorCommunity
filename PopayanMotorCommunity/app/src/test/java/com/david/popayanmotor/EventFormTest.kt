package com.david.popayanmotor

import com.david.popayanmotor.ui.EventForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventFormTest {

    @Test
    fun fechaFuturaYFin() {
        val now = "2026-10-26T13:30:45"
        assertTrue(EventForm.isFuture("2026-10-26", "13:31", now))
        assertFalse(EventForm.isFuture("2026-10-26", "13:30", now))  // mismo minuto: el backend lo rechaza
        assertFalse(EventForm.isFuture("2026-10-25", "23:59", now))
        assertTrue(EventForm.isFuture("2027-01-01", "00:00", now))
        assertTrue(EventForm.isValidEndDate("2026-11-07", null))
        assertTrue(EventForm.isValidEndDate("2026-11-07", "2026-11-07"))
        assertFalse(EventForm.isValidEndDate("2026-11-07", "2026-11-06"))
    }

    @Test
    fun numeros() {
        assertEquals(30, EventForm.capacity(" 30 "))
        assertNull(EventForm.capacity("0"))
        assertNull(EventForm.capacity(""))
        assertEquals(0, EventForm.price(""))  // vacío = gratis
        assertEquals(25000, EventForm.price("25000"))
        assertNull(EventForm.price("-1"))
        assertTrue(EventForm.isValidDistance(""))  // opcional
        assertEquals(12.5, EventForm.distance("12,5")!!, 0.0)
        assertFalse(EventForm.isValidDistance("0"))
        assertFalse(EventForm.isValidDistance("abc"))
    }
}
