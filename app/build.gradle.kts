plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.secureloginapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.secureloginapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    // 🚀 AÑADIR ESTAS LÍNEAS PARA TENSORFLOW LITE

    // El motor principal de TensorFlow Lite (TFLite)
    implementation("org.tensorflow:tensorflow-lite:2.16.1")

    // La librería de "Soporte" que nos facilita MUCHO
    // pasar imágenes (de la cámara) al modelo.
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    //prime linea donde ya implemente el ml kit de google
    implementation("com.google.mlkit:face-detection:16.1.7")
    // 2. Las librerías de CameraX (que siguen siendo 1.3.3)
    implementation("androidx.camera:camera-core:1.3.3")
    implementation("androidx.camera:camera-camera2:1.3.3")
    implementation("androidx.camera:camera-lifecycle:1.3.3")
    implementation("androidx.camera:camera-view:1.3.3")
    //segunda linea donde se agrega una libreria para tener botones modernos
    implementation("com.google.android.material:material:1.12.0")
// Material Design
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    //nuevas lineas de codigo
//nuevas lineas de codigo (CORREGIDO)
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")

// 🚀 CAMBIO: Le damos a Firestore y Analytics sus propias versiones
// (compatibles con las versiones de Auth/Database que ya tienes)
    implementation("com.google.firebase:firebase-firestore-ktx:25.0.0")
    implementation("com.google.firebase:firebase-analytics-ktx:22.0.0")

// 🚀 CAMBIO: Eliminamos los BOMs conflictivos
// implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
// implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
// implementation("com.google.firebase:firebase-analytics") // <-- Movido arriba con versión


    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries
    apply(plugin = "com.google.gms.google-services")

}
