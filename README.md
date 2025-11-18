# SecureFace Login: App Android con TFLite y Firebase

Este es un proyecto de Android Studio (Kotlin) que implementa un sistema de **autenticación de 3 factores** (Email/Pass + Rostro).

El objetivo fue integrar un modelo de **TensorFlow Lite (`.tflite`)** para añadir seguridad biométrica a un login estándar de Firebase.

## 🚀 Características

* **Firebase Authentication:** Para registro y login con Email/Contraseña.
* **Google ML Kit:** Para la *detección* de rostros en tiempo real desde la cámara.
* **TensorFlow Lite:** Para la *verificación*. Usamos un modelo `mobilefacenet.tflite` (160x160) para generar un "embedding" (huella facial) de 128 dimensiones.
* **Cloud Firestore:** Para *almacenar* la huella facial de cada usuario.
* **Lógica de Comparación:** La app calcula la distancia entre la huella guardada y la huella en vivo, y la valida contra un umbral (ej. `3.0f`) para verificar la identidad.

## 🛠️ Tecnologías Utilizadas

* Kotlin
* Firebase (Authentication y Cloud Firestore)
* TensorFlow Lite (Intérprete)
* Google ML Kit (Face Detection)
* Android Jetpack (CameraX)

## 📸 Flujo de la App

1.  **Registro:** El usuario se registra (Auth), la app genera su huella facial (TFLite) y la guarda (Firestore).
2.  **Login:** El usuario ingresa (Auth), la app descarga la huella (Firestore), escanea su rostro (TFLite) y compara las huellas para verificar.