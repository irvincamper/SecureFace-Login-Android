package com.example.secureloginapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var btnFaceRecognition: Button
    private lateinit var registerText: TextView

    private var isFaceVerified = false
    private lateinit var auth: FirebaseAuth

    private lateinit var db: FirebaseFirestore

    private lateinit var faceVerificationLauncher: ActivityResultLauncher<Intent>

    private var savedFaceEmbedding: FloatArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicialización
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("FIREBASE_ERROR", "Error al inicializar Firebase: ${e.message}")
        }
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Referencias del layout
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        btnLogin = findViewById(R.id.btnLogin)
        btnFaceRecognition = findViewById(R.id.btnFaceRecognition)
        registerText = findViewById(R.id.registerText)

        // Launcher
        faceVerificationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                isFaceVerified = true
                Toast.makeText(this, "Rostro reconocido ✅", Toast.LENGTH_SHORT).show()
                Log.d("FACE_LOGIN", "Resultado OK de FaceVerificationActivity (Login)")
                performFirebaseLogin()
            } else {
                isFaceVerified = false
                Toast.makeText(this, "Rostro no reconocido o cancelado 😞", Toast.LENGTH_SHORT).show()
                Log.d("FACE_LOGIN", "Resultado CANCELED de FaceVerificationActivity (Login)")
                auth.signOut()
            }
        }

        // Botón de Verificación Facial
        btnFaceRecognition.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, completa email y contraseña 📝", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result?.user != null) {
                        Log.d("FACE_LOGIN", "Email/Pass correctos.")
                        fetchFaceEmbedding(task.result.user!!)
                    } else {
                        Log.w("FACE_LOGIN", "Email o Pass incorrectos.", task.exception)
                        Toast.makeText(this, "Email o Contraseña incorrectos ❌", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Botón de Login
        btnLogin.setOnClickListener {
            performFirebaseLogin()
        }

        // Ir al registro
        registerText.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchFaceEmbedding(user: FirebaseUser) {
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val embeddingList = document.get("faceEmbedding") as? List<Double>

                    if (embeddingList != null) {
                        Log.d("FACE_LOGIN", "Huella facial descargada de Firestore.")

                        savedFaceEmbedding = FloatArray(embeddingList.size) { i ->
                            embeddingList[i].toFloat()
                        }

                        launchFaceVerification()

                    } else {
                        Log.e("FACE_LOGIN", "Error: El usuario existe pero no tiene huella (faceEmbedding).")
                        Toast.makeText(this, "Error: Tu cuenta no tiene rostro registrado 🧐", Toast.LENGTH_LONG).show()
                        auth.signOut()
                    }
                } else {
                    Log.e("FACE_LOGIN", "Error: No se encontró documento en Firestore para el UID ${user.uid}")
                    auth.signOut()
                }
            }
            .addOnFailureListener { e ->
                Log.e("FACE_LOGIN", "Error al descargar huella de Firestore", e)
                Toast.makeText(this, "Error al conectar con la base de datos 🔌", Toast.LENGTH_SHORT).show()
                auth.signOut()
            }
    }

    private fun launchFaceVerification() {
        val intent = Intent(this, FaceVerificationActivity::class.java)
        intent.putExtra("MODE", "LOGIN")
        intent.putExtra("savedEmbedding", savedFaceEmbedding)
        faceVerificationLauncher.launch(intent)
    }

    private fun performFirebaseLogin() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos ✍️", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isFaceVerified) {
            Toast.makeText(this, "¡Debes verificar tu rostro primero! 👀", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("FACE_LOGIN", "¡ÉxITO TOTAL! Email, Pass y Rostro verificados.")
        Toast.makeText(this, "¡Inicio de sesión exitoso! 🎉", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
