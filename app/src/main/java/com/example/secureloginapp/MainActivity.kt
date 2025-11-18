package com.example.secureloginapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        val logoutBtn = findViewById<Button>(R.id.logoutBtn)

        val user = auth.currentUser

        // Mensaje de bienvenida personalizado
        welcomeText.text = "¡Hola, ${user?.email}! 👋\nBienvenido a la App Segura 🥳"

        // Acción al presionar "Cerrar sesión"
        logoutBtn.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Has cerrado sesión. ¡Vuelve pronto! 👋", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Cierra la pantalla actual
        }
    }
}