package com.example.thigiuaki

import android.app.Application
import com.example.thigiuaki.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory


class appCheck : Application() {

    override fun onCreate() {
        super.onCreate()

        // **KHỞI TẠO APP CHECK**
        // Đảm bảo Firebase được khởi tạo trước.
        FirebaseApp.initializeApp(this)

        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        // Kiểm tra nếu đang ở chế độ DEBUG (Để dùng Debug Provider)
        if (BuildConfig.DEBUG) {
            // SỬ DỤNG DEBUG PROVIDER
            // Điều này sẽ in ra Debug Token trong Logcat
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            //
        } else {
            // SỬ DỤNG PLAY INTEGRITY (hoặc một provider khác) cho Production
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}