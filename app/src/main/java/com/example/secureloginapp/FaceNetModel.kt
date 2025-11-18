package com.example.secureloginapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer

class FaceNetModel(context: Context) {

    // 1. El intérprete de TFLite que ejecutará el modelo
    private val interpreter: Interpreter

    // 2. El procesador de imágenes que ajustará nuestras imágenes al tamaño del modelo
    private val imageProcessor: ImageProcessor

    // 3. El nombre de nuestro modelo en la carpeta 'assets'
    private val modelName = "mobilefacenet.tflite" // ¡Este nombre es correcto!

    // 4. Dimensiones que el modelo espera (160x160 es correcto)
    private val inputImageWidth = 160
    private val inputImageHeight = 160

    init {
        // Inicializa el intérprete
        val options = Interpreter.Options()
        options.setNumThreads(4) // Usa 4 hilos para procesar
        interpreter = Interpreter(FileUtil.loadMappedFile(context, modelName), options)

        // Inicializa el procesador de imágenes
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        Log.d("FaceNetModel", "Modelo TFLite cargado e inicializado.")
    }

    /**
     * Genera el "embedding" (huella facial) a partir de un Bitmap de un rostro.
     */
    fun getFaceEmbedding(image: Bitmap): FloatArray {
        // 1. Pre-procesar la imagen (ajustar tamaño)
        var tensorImage = TensorImage.fromBitmap(image)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. Normalizar la imagen
        val byteBuffer = normalizeBitmap(tensorImage.bitmap)

        // 3. Definir el array de salida (128 es correcto)
        val faceEmbedding = Array(1) { FloatArray(128) }

        // 4. Ejecutar el modelo
        interpreter.run(byteBuffer, faceEmbedding)

        // 5. Devolver el embedding
        return faceEmbedding[0]
    }

    /**
     * Normaliza el Bitmap. FaceNet espera valores de píxeles entre -1.0 y 1.0.
     */
    private fun normalizeBitmap(bitmap: Bitmap): ByteBuffer {
        val imgData = ByteBuffer.allocateDirect(1 * inputImageWidth * inputImageHeight * 3 * 4) // 3 canales (RGB), 4 bytes por float
        imgData.order(java.nio.ByteOrder.nativeOrder())

        val pixels = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        imgData.rewind()
        for (i in 0 until inputImageWidth) {
            for (j in 0 until inputImageHeight) {
                val pixelValue = pixels[i * inputImageWidth + j]
                imgData.putFloat(((pixelValue shr 16 and 0xFF) - 127.5f) / 128.0f) // Rojo
                imgData.putFloat(((pixelValue shr 8 and 0xFF) - 127.5f) / 128.0f)  // Verde
                imgData.putFloat(((pixelValue and 0xFF) - 127.5f) / 128.0f)       // Azul
            }
        }
        return imgData

    } // 🚀 ¡AQUÍ TERMINA LA FUNCIÓN normalizeBitmap!


    /**
     * 🚀 FUNCIÓN MOVIDA (Ahora está en el lugar correcto)
     * Compara dos "huellas faciales" (embeddings) y devuelve la distancia.
     * Cuanto más bajo el número, más parecidos son los rostros.
     */
    fun compareEmbeddings(embedding1: FloatArray, embedding2: FloatArray): Float {
        var distance = 0.0f
        for (i in embedding1.indices) {
            val diff = embedding1[i] - embedding2[i]
            distance += diff * diff
        }
        return Math.sqrt(distance.toDouble()).toFloat()
    }


} // <-- Este es el '}' final de la CLASE