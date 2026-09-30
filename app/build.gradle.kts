import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Conecta la aplicación con el proyecto de Firebase a partir del archivo
    // app/google-services.json. Sin ese archivo el complemento detiene la
    // compilación, así que ambos viajan juntos en el repositorio.
    alias(libs.plugins.google.services)
}

/**
 * Datos de firma. Si el archivo no existe, el proyecto sigue compilando y la
 * variante de release queda sin firmar, para que nadie quede bloqueado.
 */
val propiedadesDeFirma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val hayFirma = propiedadesDeFirma.getProperty("storeFile")?.let {
    rootProject.file(it).exists()
} == true

android {
    namespace = "com.vozinha.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vozinha.app"
        minSdk = 24
        targetSdk = 36
        // Control de versiones: versionCode identifica la publicación ante la
        // tienda y siempre sube; versionName es lo que ve la persona usuaria.
        versionCode = 3
        versionName = "3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hayFirma) {
                storeFile = rootProject.file(propiedadesDeFirma.getProperty("storeFile"))
                storePassword = propiedadesDeFirma.getProperty("storePassword")
                keyAlias = propiedadesDeFirma.getProperty("keyAlias")
                keyPassword = propiedadesDeFirma.getProperty("keyPassword")
                // La versión mínima admitida es Android 7.0, que ya entiende
                // el esquema v2, así que la firma v1 del JAR no hace falta.
                // v3 agrega la rotación de claves en Android 9 y superiores.
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            if (hayFirma) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            // Robolectric levanta el entorno de Android dentro de la JVM, así
            // que las pruebas locales necesitan acceso a los recursos.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Extensiones KTX: agregan a las APIs de Android una versión idiomática de
    // Kotlin, con funciones de extensión, lambdas y corrutinas.
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    // Firebase: autenticación y base de datos para la persistencia.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
