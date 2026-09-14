plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.expedicion.app"
    compileSdk = 35

    defaultConfig {
        // Convencion de la organizacion (com.escorial.<app>, idem trazabilidad/pallet_cocinas).
        // El `namespace` sigue siendo com.expedicion.app: es el paquete del codigo fuente y de
        // BuildConfig/R, y no necesita coincidir con el applicationId (identidad de instalacion).
        applicationId = "com.escorial.expedicion"
        minSdk = 26
        targetSdk = 35
        // Fuente de verdad de la version: el CI la lee, la incrementa segun el label del PR
        // (breaking/feature/bugfix) y la commitea de vuelta. Formato obligatorio X.Y.Z[-dev]:
        // en `dev` lleva el sufijo -dev (prerelease), en `main` se promueve a X.Y.Z estable.
        // El versionCode lo calcula el CI (build-number + major*10000 + minor*100 + patch);
        // no editarlos a mano.
        versionCode = 10203
        versionName = "0.2.0-rc.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // URL base del API, configurable sin tocar codigo: -PAPI_BASE_URL=http://host:puerto/
        // en gradle.properties local o linea de comandos. Default apunta al emulador Android
        // contra un API corriendo en localhost (10.0.2.2 es el alias del host desde el emulador).
        val apiBaseUrl = (project.findProperty("API_BASE_URL") as String?) ?: "http://10.0.2.2:3000/"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    packaging {
        resources.excludes.add("META-INF/*")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    // Servidor HTTP local para ejercitar ApiProbe contra respuestas reales (no mockeadas).
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

