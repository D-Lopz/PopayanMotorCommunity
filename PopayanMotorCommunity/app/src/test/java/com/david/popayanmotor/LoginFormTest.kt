package com.david.popayanmotor

import com.david.popayanmotor.ui.LoginForm
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginFormTest {

    @Test
    fun dominioAutocompletado() {
        assertEquals("david@popayanmotor.com", LoginForm.email("david"))
        assertEquals("david@popayanmotor.com", LoginForm.email("  david  "))
        assertEquals("david@popayanmotor.com", LoginForm.email("david@popayanmotor.com"))  // no se duplica
        assertEquals("viejo@gmail.com", LoginForm.email("viejo@gmail.com"))  // cuenta antigua con otro dominio
        assertEquals("david@", LoginForm.email("david@"))  // incompleto: lo rechaza la validación del correo
        assertEquals("", LoginForm.email("   "))
    }
}
