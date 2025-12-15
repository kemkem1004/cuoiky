plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // ✅ Plugin Compose Compiler (Cần thiết cho Compose)
    id("org.jetbrains.kotlin.plugin.compose")
    // ✅ Plugin Google Services (Cần thiết cho Firebase)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.thigiuaki"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.thigiuaki"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        // Tùy chọn: Bạn có thể xóa dòng này nếu bạn chắc chắn BOM mới nhất đã tự động xử lý.
        // Giữ lại để đảm bảo tính tương thích với Kotlin đã chọn.
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    // =================================================================
    // 1. CÁC NỀN TẢNG (BOMs)
    // =================================================================

    // ✅ Compose BOM (Quản lý phiên bản Compose)
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))

    // ✅ Firebase BOM (Quản lý phiên bản Firebase)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))

    // =================================================================
    // 2. ANDROIDX CORE & LIFECYCLE
    // =================================================================

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    // =================================================================
    // 3. COMPOSE UI & MATERIAL 3
    // =================================================================

    implementation("androidx.compose.material3:material3")
    // 💡 LƯU Ý: Xóa số phiên bản để Compose BOM tự quản lý.
    // Nếu bạn vẫn gặp lỗi Unresolved reference: 'ChevronRight', hãy thử đổi
    // dependency này thành một phiên bản cụ thể (ví dụ: :1.6.1)

    implementation("androidx.compose.material:material-icons-extended")

    // UI cơ bản & Preview
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // =================================================================
    // 4. FIREBASE
    // =================================================================

    // Firebase Core Services (Dùng phiên bản KTX)
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // App Check (Bảo mật)
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.compose.material)
    debugImplementation("com.google.firebase:firebase-appcheck-debug") // Chỉ dùng trong debug

    // Sử dụng Version Catalog (libs)
    implementation(libs.firebase.database)
    implementation(libs.firebase.analytics)

    // =================================================================
    // 5. THƯ VIỆN BÊN NGOÀI
    // =================================================================

    // Thư viện Coil (Hiển thị ảnh)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Thư viện Material truyền thống (Nếu cần dùng View System)
    implementation("com.google.android.material:material:1.11.0")

    // =================================================================
    // 6. DEBUG & TESTING
    // =================================================================

    // Debug tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    // androidTest sử dụng cùng BOM để đảm bảo tương thích
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.3")

}