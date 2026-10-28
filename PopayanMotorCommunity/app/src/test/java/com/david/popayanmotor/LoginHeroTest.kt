package com.david.popayanmotor

import com.david.popayanmotor.ui.LoginHero
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LoginHeroTest {

    @Test
    fun sinFotosOUnaSola() {
        assertNull(LoginHero.next(0, -1))       // solo el degradado
        assertEquals(0, LoginHero.next(1, -1))  // primera apertura
        assertEquals(0, LoginHero.next(1, 0))   // con una sola foto se repite (no hay otra)
    }

    @Test
    fun nuncaRepiteLaAnteriorYLasUsaTodas() {
        val random = Random(42)
        var last = -1
        val seen = mutableSetOf<Int>()
        repeat(300) {
            val next = LoginHero.next(4, last, random)!!
            assertTrue(next in 0 until 4)
            assertNotEquals(last, next)
            seen += next
            last = next
        }
        assertEquals(setOf(0, 1, 2, 3), seen)
    }

    @Test
    fun ultimaInvalidaSeIgnora() {
        // Se quitaron fotos y el índice guardado ya no existe: elige cualquiera válida.
        repeat(50) { assertTrue(LoginHero.next(2, 5)!! in 0 until 2) }
        assertEquals(1, LoginHero.next(2, 0))  // con dos fotos alterna
        assertEquals(0, LoginHero.next(2, 1))
    }
}
