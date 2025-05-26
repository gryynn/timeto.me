plugins {
    kotlin("android")
    id("com.android.application")
    kotlin("plugin.serialization") version "1.9.22"
}

// Chargement sécurisé des propriétés
fun loadLocalProperties(): java.util.Properties {
    val properties = java.util.Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(localPropertiesFile.inputStream())
    } else {
        throw GradleException("""
            Le fichier local.properties est manquant.
            Veuillez copier local.properties.template vers local.properties
            et configurer vos clés Supabase.
        """.trimIndent())
    }
    return properties
}

fun getRequiredProperty(properties: java.util.Properties, key: String): String {
    return properties.getProperty(key) ?: run {
        throw GradleException("""
            La propriété '$key' est manquante dans local.properties.
            Veuillez la configurer selon le modèle dans local.properties.template
        """.trimIndent())
    }
}

val localProperties = loadLocalProperties()

android {
    namespace = "me.timeto.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "me.timeto.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 566
        versionName = "2024.09.05"

        // Room schema location
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
        }

        // Configuration Supabase sécurisée
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${getRequiredProperty(localProperties, "SUPABASE_URL")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_KEY",
            "\"${getRequiredProperty(localProperties, "SUPABASE_KEY")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // Protection supplémentaire en production
            buildConfigField("boolean", "ENABLE_SUPABASE_EXPORT", "true")
        }
        debug {
            // Configuration de développement
            buildConfigField("boolean", "ENABLE_SUPABASE_EXPORT", "true")
        }
    }

    flavorDimensions += "type"
    productFlavors {
        create("base") {
            dimension = "type"
        }
        create("fdroid") {
            dimension = "type"
            // Désactiver Supabase pour F-Droid
            buildConfigField("boolean", "ENABLE_SUPABASE_EXPORT", "false")
        }
    }

    applicationVariants.all {
        outputs.all {
            this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            outputFileName = "$name.apk"
        }
    }

    // https://f-droid.org/en/docs/Reproducible_Builds/#png-crushcrunch
    packaging.resources { aaptOptions.cruncherEnabled = false }

    compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    compileOptions.targetCompatibility = JavaVersion.VERSION_17

    buildFeatures.compose = true
    buildFeatures.buildConfig = true

    composeOptions.kotlinCompilerExtensionVersion = "1.5.14"
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.material:material:1.7.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // WorkManager
    val workVersion = "2.9.0"
    implementation("androidx.work:work-runtime-ktx:$workVersion")

    // Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.1.3")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
