package com.example.thigiuaki.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Định nghĩa lại màu sắc từ RoleSelectionScreen để dễ quản lý
private val BackgroundLight = Color(0xFFFAF9F6) // Màu Kem nhạt (Nền)
private val PrimaryMaroon = Color(0xFF8D021F) // Màu Đỏ Gạch (Primary)
private val SecondaryDark = Color(0xFF424242) // Xám đậm (Tiêu đề)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val handleRegister: () -> Unit = {
        isLoading = true
        errorMessage = null

        when {
            name.isBlank() -> errorMessage = "Vui lòng nhập tên của bạn."
            email.isBlank() -> errorMessage = "Vui lòng nhập email."
            password.isBlank() -> errorMessage = "Vui lòng nhập mật khẩu."
            password.length < 6 -> errorMessage = "Mật khẩu phải có ít nhất 6 ký tự."
            password != confirmPassword -> errorMessage = "Mật khẩu xác nhận không khớp."
            else -> {
                auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null) {
                                // Create user document in Firestore
                                val userData = hashMapOf(
                                    "name" to name.trim(),
                                    "email" to email.trim(),
                                    "phone" to phone.trim(),
                                    "role" to "user"
                                )
                                db.collection("users").document(user.uid)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        Log.d("Register", "User created successfully")
                                        onRegisterSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        errorMessage = "Lỗi tạo tài khoản: ${e.localizedMessage}"
                                        // Xóa tài khoản Firebase vừa tạo để tránh tài khoản lỗi
                                        user.delete()
                                        isLoading = false
                                    }
                            }
                        } else {
                            // Cung cấp thông báo lỗi rõ ràng hơn
                            val errorText = when (task.exception?.localizedMessage) {
                                "The email address is already in use by another account." -> "Email này đã được sử dụng."
                                "The email address is badly formatted." -> "Email không đúng định dạng."
                                else -> "Đăng ký thất bại: ${task.exception?.localizedMessage}"
                            }
                            errorMessage = errorText
                            isLoading = false
                        }
                    }
            }
        }
        if (errorMessage != null) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đăng ký") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = PrimaryMaroon
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = SecondaryDark
                )
            )
        },
        containerColor = BackgroundLight // Nền chính Kem nhạt
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState) // Cho phép cuộn
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            // Tiêu đề lớn và đậm
            Text(
                "Tạo Tài khoản mới",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                ),
                color = PrimaryMaroon
            )
            Text(
                "Đăng ký để bắt đầu hành trình mua sắm của bạn.",
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryDark.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(48.dp))

            // ==========================================================
            // Trường nhập liệu (Input Fields)
            // ==========================================================

            // Họ và tên
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Họ và tên") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Name Icon", tint = PrimaryMaroon) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryMaroon,
                    focusedLabelColor = PrimaryMaroon,
                    cursorColor = PrimaryMaroon
                )
            )
            Spacer(Modifier.height(16.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Địa chỉ Email") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email Icon", tint = PrimaryMaroon) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryMaroon,
                    focusedLabelColor = PrimaryMaroon,
                    cursorColor = PrimaryMaroon
                )
            )
            Spacer(Modifier.height(16.dp))

            // Số điện thoại
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Số điện thoại (Tùy chọn)") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = "Phone Icon", tint = PrimaryMaroon) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryMaroon,
                    focusedLabelColor = PrimaryMaroon,
                    cursorColor = PrimaryMaroon
                )
            )
            Spacer(Modifier.height(16.dp))

            // Mật khẩu
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu (ít nhất 6 ký tự)") },
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
            Spacer(Modifier.height(16.dp))

            // Xác nhận mật khẩu
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Xác nhận mật khẩu") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Confirm Password Icon", tint = PrimaryMaroon) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryMaroon,
                    focusedLabelColor = PrimaryMaroon,
                    cursorColor = PrimaryMaroon
                )
            )
            Spacer(Modifier.height(24.dp))

            if (errorMessage != null) {
                Text(
                    errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Nút Đăng ký (Primary Button)
            Button(
                onClick = handleRegister,
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
                    Text("ĐĂNG KÝ TÀI KHOẢN", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }

            Spacer(Modifier.height(32.dp))

            // Nút Quay lại Đăng nhập
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Đã có tài khoản? Quay lại Đăng nhập",
                    color = PrimaryMaroon.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}