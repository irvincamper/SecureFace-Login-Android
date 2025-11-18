package com.example.secureloginapp

import android.graphics.*
import android.media.Image
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

// Contiene funciones de ayuda para manipular imágenes
object ImageUtils {

    /**
     * Convierte un ImageProxy (formato YUV de la cámara) a un Bitmap (formato ARGB).
     */
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val image = imageProxy.image ?: throw IllegalStateException("Image is null")

        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copia Y
        yBuffer[nv21, 0, ySize]
        // Copia U (con un paso de 2)
        vBuffer[nv21, ySize, vSize]
        // Copia V (con un paso de 2)
        uBuffer[nv21, ySize + vSize, uSize]


        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    /**
     * Recorta un Bitmap a una caja delimitadora (Bounding Box) específica.
     */
    fun cropBitmap(bitmap: Bitmap, boundingBox: Rect): Bitmap {
        return Bitmap.createBitmap(
            bitmap,
            boundingBox.left,
            boundingBox.top,
            boundingBox.width(),
            boundingBox.height()
        )
    }
}