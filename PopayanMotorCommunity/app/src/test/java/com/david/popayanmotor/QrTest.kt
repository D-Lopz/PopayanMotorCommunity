package com.david.popayanmotor

import com.david.popayanmotor.ui.Qr
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class QrTest {

    /** Lee el QR como lo haría un lector: debe devolver exactamente el código, sin nada más. */
    private fun read(code: String, size: Int = 300): String {
        val m = Qr.matrix(code, size)
        val pixels = IntArray(m.width * m.height) { i -> if (m[i % m.width, i / m.width]) 0xFF000000.toInt() else -1 }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(m.width, m.height, pixels)))
        return QRCodeReader().decode(bitmap).text
    }

    @Test
    fun contenidoEsExactamenteElCodigo() {
        assertEquals("PMC-LEL4CD", read("PMC-LEL4CD"))
        assertEquals("PMC-AB23CD", read("PMC-AB23CD", size = 120))  // tamaño de la tarjeta
    }

    @Test
    fun codigosDistintosDanQrDistintos() {
        assertNotEquals(Qr.matrix("PMC-AAAAAA", 200), Qr.matrix("PMC-AAAAAB", 200))
    }
}
