import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
}
// Firebase (hesap + bulut yedeği): ayar dosyası yalnız bu bilgisayarda (depoya girmez). Yoksa (ör. iOS bulut derlemesi) eklenti uygulanmaz.
if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    // iPhone: derleme yalnız Mac'te (bulut Mac / GitHub Actions macOS) yapılır
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "ComposeApp"; isStatic = true }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.10.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
            // Hesap: Firebase Authentication (misafir, e-posta, Google) + buluta ilerleme yedeği (Firestore yedek/{uid})
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.7.0"))
            implementation("com.google.firebase:firebase-auth")
            implementation("com.google.firebase:firebase-firestore")
            // Google ile giriş: Android Credential Manager
            implementation("androidx.credentials:credentials:1.3.0")
            implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
            implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
            // Reklam: AdMob (alt şerit + test bitince ara sıra tam ekran; test/sınav sırasında yok)
            implementation("com.google.android.gms:play-services-ads:23.6.0")
        }
    }
}

compose.resources { packageOfResClass = "com.celikgames.ehliyetol.kaynak" }

val sifreDosya = rootProject.file("../../android/imza_sifre.txt")
val sifre = if (sifreDosya.exists()) sifreDosya.readText().trim() else ""

android {
    namespace = "com.celikgames.ehliyetol"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.celikgames.ehliyetol"   // Play'deki Ehliyet Ol ile aynı: güncelleme olarak gider
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "2.0"
        buildConfigField("boolean", "REKLAM_TEST", "true")
        // AdMob uygulama kimliği (gerçek "Ehliyet Ol"); REKLAM_TEST=true iken birimler test birimidir
        manifestPlaceholders["admobUygulama"] = "ca-app-pub-6396011409934303~9649126747"
    }
    buildFeatures { buildConfig = true }
    // Play'deki sürümle aynı imza anahtarı (takma ad eo)
    signingConfigs {
        create("yayin") {
            storeFile = rootProject.file("../../android/imza.jks")
            storePassword = sifre
            keyAlias = "eo"
            keyPassword = sifre
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("yayin"); isMinifyEnabled = false; manifestPlaceholders["uygAd"] = "Ehliyet Ol"
            buildConfigField("boolean", "REKLAM_TEST", "false")
        }
        // Deneme sürümü telefonda ayrı uygulama olarak durur (Play'den kurulu Ehliyet Ol'a dokunmaz)
        getByName("debug") { signingConfig = signingConfigs.getByName("yayin"); applicationIdSuffix = ".deneme"; versionNameSuffix = "-deneme"; manifestPlaceholders["uygAd"] = "Ehliyet Ol K" }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    androidResources { noCompress += "webp" }
    lint { abortOnError = false; checkReleaseBuilds = false }
}
