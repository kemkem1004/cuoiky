
package com.example.thigiuaki.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.thigiuaki.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Sử dụng LaunchedEffect để chạy một coroutine khi Composable được khởi tạo
    LaunchedEffect(key1 = true) {
        // Thời gian chờ, ví dụ 2000 mili giây (2 giây)
        delay(2000L)
        // Gọi callback để chuyển màn hình
        onTimeout()
    }

    // Hiển thị giao diện Splash Screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Thay đổi màu nền cho phù hợp với logo của bạn (ví dụ: màu đỏ đậm)
            .background(Color(0xFF8B0000)), // Màu đỏ sẫm (Dark Red) hoặc màu bạn muốn
        contentAlignment = Alignment.Center
    ) {
        // Thay thế R.drawable.ic_app_logo bằng resource ID của logo thực tế của bạn
        // Ví dụ: R.drawable.ic_logo_mono (nếu bạn đã tạo)
        Image(
            painter = painterResource(id = R.drawable.logo1), // Thay thế ID này
            contentDescription = "App Logo",
            modifier = Modifier.size(200.dp) // Điều chỉnh kích thước logo
        )
    }
}