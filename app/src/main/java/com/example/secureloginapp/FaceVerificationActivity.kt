package com.example.secureloginapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceVerificationActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceDetector: FaceDetector
    private var isDetectionDone = false

    // Variables para el "cerebro" (TFLite) y el modo
    private lateinit var faceNetModel: FaceNetModel
    private var mode: String = "LOGIN"

    // 🚀 CAMBIO: Variable para guardar la huella que recibimos del Login
    private var savedEmbedding: FloatArray? = null

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_verification)

        viewFinder = findViewById(R.id.viewFinder)

        // 1. Obtenemos el modo (REGISTER o LOGIN)
        mode = intent.getStringExtra("MODE") ?: "LOGIN"
        Log.d("FaceVerify", "Modo de operación: $mode")

        // 🚀 CAMBIO: Si estamos en modo LOGIN, obtenemos la huella guardada
        if (mode == "LOGIN") {
            savedEmbedding = intent.getFloatArrayExtra("savedEmbedding")

            // Verificación de seguridad
            if (savedEmbedding == null) {
                Log.e("FaceVerify", "Modo LOGIN pero no se recibió 'savedEmbedding'. Abortando.")
                Toast.makeText(this, "Error: No se recibió la huella guardada", Toast.LENGTH_LONG).show()
                finish() // Cerramos la actividad si no hay huella
                return
            } else {
                Log.d("FaceVerify", "Huella guardada recibida, tamaño: ${savedEmbedding?.size}")
            }
        }

        // 2. Inicializamos el "cerebro" TFLite
        faceNetModel = FaceNetModel(this)

        // 3. Inicializa el detector de rostros de ML Kit
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
        faceDetector = FaceDetection.getClient(highAccuracyOpts)

        // 4. Preparamos el hilo de la cámara
        cameraExecutor = Executors.newSingleThreadExecutor()

        // 5. Verificar permisos
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    // Revisa si el permiso de CÁMARA ha sido concedido
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // Función que se llama después de que el usuario responde a la solicitud de permiso
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permiso de cámara denegado.", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    // Configura e inicia CameraX
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("CAMERA_ERROR", "Fallo al vincular casos de uso de la cámara", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    // -------------------------------------------------------------------------
    // 🚀 FUNCIÓN PRINCIPAL (MODIFICADA PARA LA FASE 7.7)
    // -------------------------------------------------------------------------
    @androidx.camera.core.ExperimentalGetImage
    private fun processImage(imageProxy: ImageProxy) {
        if (isDetectionDone) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        // ¡SE DETECTÓ UN ROSTRO!
                        isDetectionDone = true
                        cameraExecutor.shutdown()
                        val face = faces[0] // Tomamos el primer rostro

                        try {
                            // 1. Convertir imagen de la cámara a Bitmap
                            val bitmap = ImageUtils.imageProxyToBitmap(imageProxy)

                            // 2. Recortar el Bitmap al rostro
                            val croppedFace = ImageUtils.cropBitmap(bitmap, face.boundingBox)

                            // 3. Generar la "huella" del rostro EN VIVO (Live)
                            val liveEmbedding = faceNetModel.getFaceEmbedding(croppedFace)
                            Log.d("FaceNet", "Huella EN VIVO generada, tamaño: ${liveEmbedding.size}")

                            // 4. Decidir qué hacer (Registrar o Comparar)
                            runOnUiThread {

                                if (mode == "REGISTER") {
                                    // -------- MODO REGISTRO --------
                                    // Simplemente devolvemos la huella generada
                                    Log.d("FaceNet", "Modo REGISTRO. Devolviendo huella.")
                                    val resultIntent = Intent()
                                    resultIntent.putExtra("faceEmbedding", liveEmbedding)
                                    setResult(RESULT_OK, resultIntent)

                                } else {
                                    // -------- MODO LOGIN --------
                                    // Comparamos la huella EN VIVO vs la huella GUARDADA
                                    Log.d("FaceNet", "Modo LOGIN. Comparando huellas...")

                                    if (savedEmbedding == null) {
                                        Log.e("FaceNet", "Error: 'savedEmbedding' es nulo en modo LOGIN.")
                                        setResult(RESULT_CANCELED)
                                    } else {

                                        // ¡LA COMPARACIÓN! (Usando la función que añadimos a FaceNetModel)
                                        val distance = faceNetModel.compareEmbeddings(savedEmbedding!!, liveEmbedding)

                                        // Definimos un "umbral".
                                        // Si la distancia es < 3f, son la misma persona.
                                        val umbral = 3f

                                        Log.d("FaceNet", "Distancia de comparación: $distance (Umbral: $umbral)")

                                        if (distance < umbral) {
                                            // ¡COINCIDENCIA!
                                            Log.d("FaceNet", "¡COINCIDENCIA! El rostro es el mismo.")
                                            setResult(RESULT_OK)
                                        } else {
                                            // ¡NO COINCIDENCIA!
                                            Log.d("FaceNet", "¡FALLO! El rostro es diferente.")
                                            setResult(RESULT_CANCELED)
                                        }
                                    }
                                }
                                finish() // Cerramos la pantalla y devolvemos el resultado
                            }

                        } catch (e: Exception) {
                            Log.e("FaceNet_ERROR", "Error al procesar el embedding: ${e.message}")
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MLKIT_ERROR", "Error en ML Kit: ${e.message}")
                }
                .addOnCompleteListener {
                    imageProxy.close() // ¡Importante!
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
        }
    }
}