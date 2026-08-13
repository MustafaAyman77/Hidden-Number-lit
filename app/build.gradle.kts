import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.secrets)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlinx.serialization)
}

// ✅ قراءة local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

// ✅ قراءة المفاتيح
val keystorePath: String = System.getenv("KEYSTORE_PATH") 
    ?: localProperties.getProperty("KEYSTORE_PATH") 
    ?: "${rootDir}/my-upload-key.jks"

val storePassword: String = System.getenv("STORE_PASSWORD") 
    ?: localProperties.getProperty("STORE_PASSWORD") 
    ?: ""

val keyPassword: String = System.getenv("KEY_PASSWORD") 
    ?: localProperties.getProperty("KEY_PASSWORD") 
    ?: ""

android {
    namespace = "com.aistudio.hiddennumber.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.hiddennumber.game"
        minSdk = 24
        targetSdk = 36
        versionCode = 10069
        versionName = "1.0.69"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        val supabaseUrl: String = project.properties["SUPABASE_URL"] as? String ?: ""
        val supabaseAnonKey: String = project.properties["SUPABASE_ANON_KEY"] as? String ?: ""
        
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "VERSION_NAME", "\"${defaultConfig.versionName}\"")
        buildConfigField("Int", "VERSION_CODE", "${defaultConfig.versionCode}")
    }

    signingConfigs {
        val releaseKeystoreFile = file(keystorePath)
        if (releaseKeystoreFile.exists() && storePassword.isNotEmpty() && keyPassword.isNotEmpty()) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = storePassword
                keyAlias = "upload"
                keyPassword = keyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        } else {
            println("⚠️ Release keystore not found or passwords missing.")
        }
        
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.findByName("release") 
                ?: signingConfigs.findByName("debugConfig")
        }

        debug {
            signingConfig = signingConfigs.findByName("debugConfig")
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        create("staging") {
            initWith(buildTypes.getByName("debug"))
            signingConfig = signingConfigs.findByName("debugConfig")
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            matchingFallbacks += listOf("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = false
        dataBinding = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/gradle/incremental.annotation.processors"
            excludes += "**/attach_hotspot_windows.dll"
            excludes += "META-INF/licenses/**"
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        animationsDisabled = true
    }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}

googleServices {
    missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.firebase.appcheck.recaptcha)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.logging.interceptor)
    implementation(libs.okhttp)
    implementation(libs.moshi.kotlin)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.coil.compose)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Supabase
    implementation(libs.supabase.storage)
    implementation(libs.supabase.auth)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.dotenv)
    
    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    
    "ksp"(libs.androidx.room.compiler)
    "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("createDebugKeystore") {
    doLast {
        val debugKeystore = file("${rootDir}/debug.keystore")
        if (!debugKeystore.exists()) {
            println("🔑 Creating debug.keystore...")
            exec {
                commandLine(
                    "keytool", "-genkey", "-v",
                    "-keystore", debugKeystore.absolutePath,
                    "-storepass", "android",
                    "-alias", "androiddebugkey",
                    "-keypass", "android",
                    "-dname", "CN=Android Debug,O=Android,C=US",
                    "-keyalg", "RSA",
                    "-keysize", "2048",
                    "-validity", "10000"
                )
            }
            println("✅ debug.keystore created successfully!")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("createDebugKeystore")
}
