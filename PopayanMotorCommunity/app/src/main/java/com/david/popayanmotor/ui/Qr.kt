package com.david.popayanmotor.ui

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * QR de una inscripción. El contenido es EXACTAMENTE el código ("PMC-AB12CD"): sin token, correo ni ids.
 * En la entrada el admin lo valida con GET /api/registrations/verify?code=&event_id= (el QR solo no prueba nada).
 */
object Qr {

    /** Matriz del QR (sin Android, para probarla con JUnit). Corrección M: se lee aunque la pantalla tenga reflejos. */
    fun matrix(code: String, sizePx: Int): BitMatrix = QRCodeWriter().encode(
        code, BarcodeFormat.QR_CODE, sizePx, sizePx,
        mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1)
    )

    fun bitmap(code: String, sizePx: Int, @ColorInt dark: Int, @ColorInt light: Int): Bitmap {
        val m = matrix(code, sizePx)
        val pixels = IntArray(m.width * m.height) { i -> if (m[i % m.width, i / m.width]) dark else light }
        return Bitmap.createBitmap(pixels, m.width, m.height, Bitmap.Config.RGB_565)
    }
}
