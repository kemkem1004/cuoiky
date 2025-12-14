package com.example.thigiuaki.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.thigiuaki.R
import androidx.compose.ui.unit.DpOffset

@Composable
fun RoleSelectionScreen(
    onAdminSelected: () -> Unit,
    onCustomerSelected: () -> Unit
) {
    // Bảng màu hiện đại
    val BackgroundLight = Color(0xFFFAF9F6) // Màu Kem nhạt (Nền)
    val PrimaryMaroon = Color(0xFF8D021F) // Màu Đỏ Gạch (Primary)
    val SecondaryDark = Color(0xFF424242) // Xám đậm (Tiêu đề)
    val LightAccent = Color(0xFFFFE0B2) // Màu da cam nhạt cho chi tiết đồ họa

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {

        // ==========================================================
        // THÊM CHI TIẾT ĐỒ HỌA CHUYÊN NGHIỆP (Shape Layering)
        // ==========================================================

        // Hình tròn lớn 1 (Góc trên phải)
        Box(
            modifier = Modifier
                .offset(x = 100.dp, y = (-200).dp)
                .size(300.dp)
                .clip(CircleShape)
                .background(PrimaryMaroon.copy(alpha = 0.1f))
        )

        // Hình tròn lớn 2 (Góc dưới trái)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-100).dp, y = 150.dp)
                .size(250.dp)
                .clip(CircleShape)
                .background(LightAccent)
        )
        // ==========================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Tăng Padding ngang để làm sạch hai bên
                .padding(horizontal = 40.dp, vertical = 64.dp)
                .padding(bottom = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            // 1. Logo/Thương hiệu ở trên cùng
            Icon(
                imageVector = Icons.Filled.LocalMall,
                contentDescription = "Shopping Icon",
                tint = PrimaryMaroon,
                modifier = Modifier.size(60.dp).align(Alignment.Start) // Căn trái
            )

            Spacer(Modifier.height(32.dp))

            // 2. Khu vực Tiêu đề (Căn trái)
            Column(modifier = Modifier.fillMaxWidth().align(Alignment.Start)) {
                Text(
                    text = "Chào mừng bạn đến với",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    color = SecondaryDark.copy(alpha = 0.7f)
                )

                Text(
                    text = "PAD SHOP",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 48.sp
                    ),
                    color = PrimaryMaroon
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Vui lòng chọn vai trò để truy cập cửa hàng và khám phá những sản phẩm mới nhất của chúng tôi.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SecondaryDark.copy(alpha = 0.9f)
                )
            }

            // Đẩy các nút xuống dưới
            Spacer(Modifier.weight(1f))

            // 3. Các nút tương tác lớn và rõ ràng

            // Nút Khách hàng (Primary Action: Màu Đỏ Gạch)
            Button(
                onClick = onCustomerSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp), // Nút lớn hơn nữa
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryMaroon,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Customer",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "TIẾP TỤC VỚI TƯ CÁCH KHÁCH HÀNG",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Nút Quản trị viên (Secondary Action: Viền Đỏ Gạch)
            OutlinedButton(
                onClick = onAdminSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryMaroon,
                    containerColor = BackgroundLight
                ),
                border = BorderStroke(width = 2.dp, color = PrimaryMaroon)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Admin",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "ĐĂNG NHẬP VỚI QUẢN TRỊ VIÊN",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            // Thêm footer nhỏ (Thông tin thêm)
            Spacer(Modifier.height(32.dp))

        }
    }
}