plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    // أضف هذه إذا لم تكن موجودة
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.myapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // إضافة BuildConfig للمتغيرات
        buildConfigField("String", "SUPABASE_URL", "\"${project.properties["SUPABASE_URL"] ?: ""}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${project.properties["SUPABASE_ANON_KEY"] ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // إضافة المتغيرات في الـ Release
            buildConfigField("String", "SUPABASE_URL", "\"${project.properties["SUPABASE_URL"] ?: ""}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${project.properties["SUPABASE_ANON_KEY"] ?: ""}\"")
        }
        debug {
            buildConfigField("String", "SUPABASE_URL", "\"${project.properties["SUPABASE_URL"] ?: ""}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${project.properties["SUPABASE_ANON_KEY"] ?: ""}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true // تأكد من تفعيل BuildConfig
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
    
    // Supabase
    implementation(libs.supabase.storage)
    implementation(libs.supabase.auth)
    implementation(libs.ktor.client.android)
    
    // قراءة ملف .env
    implementation(libs.dotenv)
    
    // Coil للصور
    implementation("io.coil-kt:coil-compose:2.4.0")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
