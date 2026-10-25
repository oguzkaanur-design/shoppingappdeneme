// Project-level build file for Android Shopping App
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

// Room geçişlerinin doğrulanabilmesi için şemaları app/schemas altına yaz.
kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

// Kalıcı imza anahtarı: keystore.properties (git'e girmez) yoksa release imzasız kalır.
// Aynı anahtar + aynı applicationId + artan versionCode = telefonda "güncelleme" (veriler korunur).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.shoppingapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shoppingapp"
        minSdk = 26
        targetSdk = 36
        // Her yeni sürümde versionCode'u ARTIR (telefon "güncelleme" olarak algılar, veriler kalır).
        // CI / telefondan tetiklenen derlemelerde kod düzenlemeden üzerine yazılabilir:
        //   ./gradlew :app:assembleRelease -PversionCode=6 -PversionName=1.5
        versionCode = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 17
        versionName = (findProperty("versionName") as String?) ?: "3.1"
        testInstrumentationRunner = "com.shoppingapp.testing.KorbTestRunner"

        // Google ile giriş, Google Cloud'da oluşturulan **Web** OAuth istemcisinin
        // kimliğine ihtiyaç duyar. Kimlik yoksa giriş düğmesi "yapılandırılmadı"
        // durumunda kalır; uygulamanın geri kalanı etkilenmez.
        //   local.properties ya da gradle.properties: googleWebClientId=...apps.googleusercontent.com
        val googleClientId = (findProperty("googleWebClientId") as String?)
            ?: System.getenv("GOOGLE_WEB_CLIENT_ID")
            ?: ""
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
    }

    sourceSets {
        // MigrationTestHelper şema dosyalarını asset olarak okur.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8: kodu karıştırır (kopyalanmayı zorlaştırır) ve kullanılmayan kodu atar.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    bundle {
        // Uygulama içi dil seçimi tüm dilleri anında kullanabilmeli; Play'in
        // dil bazlı kaynak bölmesi kapalı. Ek metin maliyeti küçük.
        language {
            enableSplit = false
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        // 16 KB sayfa boyutlu cihazlar için native kütüphaneler sıkıştırılmadan
        // ve hizalı paketlenir; AGP 8.13 varsayılanı bunu destekler.
        jniLibs.useLegacyPackaging = false
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Hata seviyesindeki lint bulguları derlemeyi durdurur; CI de aynı eşiği kullanır.
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Jetpack Compose — sürümler BOM'dan; derleyici Kotlin 2.x Compose eklentisinden.
    implementation(platform("androidx.compose:compose-bom:2025.09.01"))
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.fragment:fragment:1.8.9")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.9.3")
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.57.2")
    kapt("com.google.dagger:hilt-compiler:2.57.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // WorkManager (hatırlatıcı bildirimleri)
    implementation("androidx.work:work-runtime-ktx:2.10.2")

    // Room Database
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    kapt("androidx.room:room-compiler:2.7.2")

    // CameraX + ML Kit
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    // Fiş OCR'ı: model cihazda değil Play Services üzerinden gelir (APK ~30 MB küçülür).
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

    // Retrofit Network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coil Image Loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Hesap: Credential Manager ile Google girişi, play-services-auth ile Drive izni
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.09.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.room:room-testing:2.7.2")
}
