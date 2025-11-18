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
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var registerBtn: Button
    private lateinit var loginRedirect: TextView

    private lateinit var faceVerificationLauncher: ActivityResultLauncher<Intent>

    private var tempUserId: String? = null
    private var tempUserEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        emailInput = findViewById(R.id.registerEmail)
        passwordInput = findViewById(R.id.registerPassword)
        registerBtn = findViewById(R.id.registerBtn)
        loginRedirect = findViewById(R.id.loginRedirect)

        faceVerificationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val embeddingArray = result.data?.getFloatArrayExtra("faceEmbedding")

                if (embeddingArray != null) {
                    val embeddingList = embeddingArray.toList()
                    Toast.makeText(this, "Rostro capturado. Guardando... ⏳", Toast.LENGTH_SHORT).show()
                    Log.d("Register", "Embedding recibido, tamaño: ${embeddingList.size}")
                    saveUserEmbedding(embeddingList)
                } else {
                    Log.e("REGISTER_FAIL", "FaceVerification OK, pero no se recibió el embedding.")
                    deleteIncompleteUser()
                }
            } else {
                Log.w("REGISTER_FAIL", "Captura de rostro cancelada. Borrando usuario...")
                deleteIncompleteUser()
            }
        }

        registerBtn.setOnClickListener {
            val userEmail = emailInput.text.toString().trim()
            val userPassword = passwordInput.text.toString().trim()

            if (userEmail.isEmpty() || userPassword.isEmpty() || userPassword.length < 6) {
                Toast.makeText(this, "Completa los campos (contraseña mín. 6 caracteres) ✍️", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("REGISTER", "Usuario creado en Auth exitosamente.")
                        tempUserId = task.result?.user?.uid
                        tempUserEmail = task.result?.user?.email
                        launchFaceVerification()
                    } else {
                        Log.e("REGISTER_FAIL", "Error al crear usuario en Auth", task.exception)
                        Toast.makeText(this, "Error al registrar: ${task.exception?.message} 😢", Toast.LENGTH_LONG).show()
                    }
                }
        }

        loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun launchFaceVerification() {
        val intent = Intent(this, FaceVerificationActivity::class.java)
        intent.putExtra("MODE", "REGISTER")
        faceVerificationLauncher.launch(intent)
    }

    private fun saveUserEmbedding(embedding: List<Float>) {
        if (tempUserId == null) return

        val userData = hashMapOf(
            "email" to tempUserEmail,
            "faceEmbedding" to embedding
        )

        db.collection("users").document(tempUserId!!)
            .set(userData)
            .addOnSuccessListener {
                Log.d("REGISTER", "Usuario y huella guardados en Firestore exitosamente.")
                Toast.makeText(this, "¡Registro completo! 🎉", Toast.LENGTH_LONG).show()
                auth.signOut()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("REGISTER_FAIL", "Error al guardar en Firestore", e)
                Toast.makeText(this, "Error al guardar datos. Borrando... 😥", Toast.LENGTH_LONG).show()
                deleteIncompleteUser()
            }
    }

    private fun deleteIncompleteUser() {
        val user = auth.currentUser
        user?.delete()?.addOnCompleteListener {
            Log.d("REGISTER_CLEANUP", "Usuario de Auth borrado.")
            Toast.makeText(this, "Registro cancelado.", Toast.LENGTH_SHORT).show()
        }
    }
}