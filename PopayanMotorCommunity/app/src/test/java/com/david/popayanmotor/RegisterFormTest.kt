package com.david.popayanmotor

import com.david.popayanmotor.ui.RegisterForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterFormTest {

    @Test
    fun correoConDominioFijo() {
        assertEquals("david.lopez@popayanmotor.com", RegisterForm.email(" David.Lopez "))
        assertEquals("david@popayanmotor.com", RegisterForm.email("david@popayanmotor.com"))  // correo completo
        assertNull(RegisterForm.email(""))
        assertNull(RegisterForm.email("david@gmail.com"))  // otro dominio
        assertNull(RegisterForm.email("david@popayanmotor.com.co"))
        assertNull(RegisterForm.email(".david"))
        assertNull(RegisterForm.email("da vid"))
    }

    @Test
    fun celularYClave() {
        assertTrue(RegisterForm.isValidPhone(""))  // opcional
        assertTrue(RegisterForm.isValidPhone("+57 315 234 9876"))
        assertFalse(RegisterForm.isValidPhone("123"))
        assertTrue(RegisterForm.isValidPassword("Clave12345"))
        assertFalse(RegisterForm.isValidPassword("corta"))
    }
}
