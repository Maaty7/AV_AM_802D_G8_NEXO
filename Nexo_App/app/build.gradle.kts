import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

/**
 * IP del backend de desarrollo — nunca hardcodeada en el código ni en un
 * recurso versionado. Se lee de local.properties (que ya está en
 * .gitignore, igual que sdk.dir), con un valor por defecto si la clave no
 * está, para que el proyecto siga compilando en un checkout nuevo.
 */
val localProperties = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) {
        FileInputStream(archivo).use { load(it) }
    }
}
val backendIpDebug: String = localProperties.getProperty("nexo.backend.ip", "127.0.0.1")

/** Genera res/xml/network_security_config.xml para debug con la IP leída arriba — nunca un archivo estático versionado. */
abstract class GenerarNetworkSecurityConfigTask : DefaultTask() {
    @get:Input
    abstract val ip: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generar() {
        val carpetaXml = outputDir.get().asFile.resolve("xml")
        carpetaXml.mkdirs()
        carpetaXml.resolve("network_security_config.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <network-security-config>
                <domain-config cleartextTrafficPermitted="true">
                    <domain includeSubdomains="false">${ip.get()}</domain>
                </domain-config>
            </network-security-config>
            """.trimIndent()
        )
    }
}

android {
    namespace = "cl.duoc.nexo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "cl.duoc.nexo"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // La IP viene de local.properties (nexo.backend.ip) — ver más
            // arriba. Solo alcanzable dentro de la red local: el
            // network_security_config de debug (generado por Gradle con esta
            // misma IP, ver GenerarNetworkSecurityConfigTask) permite tráfico
            // sin cifrar únicamente hacia ella. En release no existe ese
            // permiso: ahí solo funciona HTTPS.
            buildConfigField("String", "BASE_URL", "\"http://$backendIpDebug:8000/\"")
        }
        release {
            optimization {
                enable = false
            }
            // Placeholder: no hay backend desplegado en la nube todavía. Cuando
            // exista, reemplazar por la URL HTTPS real.
            buildConfigField("String", "BASE_URL", "\"https://api.nexo.example.com/\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        val nombreTarea = "generar${variant.name.replaceFirstChar { it.uppercase() }}NetworkSecurityConfig"
        val tareaGenerar = tasks.register(nombreTarea, GenerarNetworkSecurityConfigTask::class.java) {
            ip.set(backendIpDebug)
            outputDir.set(layout.buildDirectory.dir("generated/nexo/${variant.name}/res"))
        }
        variant.sources.res?.addGeneratedSourceDirectory(tareaGenerar, GenerarNetworkSecurityConfigTask::outputDir)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
}