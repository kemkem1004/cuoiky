package com.example.thigiuaki.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock

// Định nghĩa lại màu sắc từ RoleSelectionScreen để dễ quản lý
private val BackgroundLight = Color(0xFFFAF9F6) // Màu Kem nhạt (Nền)
private val PrimaryMaroon = Color(0xFF8D021F) // Màu Đỏ Gạch (Primary)
private val SecondaryDark = Color(0xFF424242) // Xám đậm (Tiêu đề)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onBack: () -> Unit,
    onRegisterClick: (() -> Unit)? = null

) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val handleLogin: () -> Unit = {
        isLoading = true
        errorMessage = null
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Vui lòng nhập đầy đủ email và mật khẩu."
            isLoading = false
        } else {
            auth.signInWithEmailAndPassword(email.trim(), password.trim())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            db.collection("users").document(uid).get()
                                .addOnSuccessListener { snap ->
                                    val role = snap.getString("role") ?: "user"
                                    Log.d("Auth", "User role: $role")
                                    onLoginSuccess(role)
                                }
                                .addOnFailureListener {
                                    errorMessage = "Lỗi Rules: Không thể lấy vai trò."
                                    Log.e("Auth", "Không thể lấy vai trò từ Firestore", it)
                                    onLoginSuccess("user")
                                }

                        } else {
                            errorMessage = "Không lấy được UID người dùng."
                            onLoginSuccess("user")
                        }
                    } else {
                        // Thêm logic để hiển thị lỗi cụ thể hơn cho người dùng
                        val errorText = when (task.exception?.localizedMessage) {
                            "The email address is badly formatted." -> "Email không đúng định dạng."
                            "There is no user record corresponding to this identifier. The user may have been deleted." -> "Tài khoản không tồn tại."
                            "The password is invalid." -> "Mật khẩu không chính xác."
                            else -> "Đăng nhập thất bại. Vui lòng thử lại."
                        }
                        errorMessage = errorText
                    }
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đăng nhập") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = PrimaryMaroon // Icon màu Đỏ Gạch
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight, // Nền Kem nhạt
                    titleContentColor = SecondaryDark // Tiêu đề Xám đậm
                )
            )
        },
        containerColor = BackgroundLight // Nền chính Kem nhạt
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // Căn lên trên để tránh bị che
        ) {

            Spacer(Modifier.height(32.dp))

            // Tiêu đề lớn và đậm
            Text(
                "Đăng nhập Tài khoản",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                ),
                color = PrimaryMaroon
            )
            Text(
                "Chào mừng trở lại PAD SHOP!",
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryDark.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(48.dp))

            // ==========================================================
            // Trường nhập liệu (Input Fields)
            // ==========================================================
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Địa chỉ Email") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email Icon", tint = PrimaryMaroon) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryMaroon, // Viền khi focus màu Đỏ Gạch
                    focusedLabelColor = PrimaryMaroon,
                    cursorColor = PrimaryMaroon
                )
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password Icon", tint = PrimaryMaroon) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryMaroon,
                    focusedLabelColor = PrimaryMaroon,
                    cursorColor = PrimaryMaroon
                )
            )
            Spacer(Modifier.height(8.dp))

            // Nút quên mật khẩu (Căn phải)
            TextButton(onClick = { /* TODO: Implement Forgot Password */ }, modifier = Modifier.align(Alignment.End)) {
                Text(
                    "Quên mật khẩu?",
                    color = PrimaryMaroon.copy(alpha = 0.8f)
                )
            }
            Spacer(Modifier.height(24.dp))

            if (errorMessage != null) {
                Text(
                    errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Nút Đăng nhập (Primary Button)
            Button(
                onClick = handleLogin,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryMaroon,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("ĐĂNG NHẬP", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }

            if (onRegisterClick != null) {
                Spacer(Modifier.height(32.dp))
                // Nút Đăng ký (Secondary Action)
                TextButton(
                    onClick = onRegisterClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Chưa có tài khoản? Đăng ký ngay",
                        color = PrimaryMaroon, // Màu chữ Đỏ Gạch
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }

            // Đẩy các phần tử còn lại xuống cuối màn hình
            Spacer(Modifier.weight(1f))

            // Footer nhỏ
            Text(
                "Sử dụng email và mật khẩu đã đăng ký để truy cập.",
                style = MaterialTheme.typography.labelMedium,
                color = SecondaryDark.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}