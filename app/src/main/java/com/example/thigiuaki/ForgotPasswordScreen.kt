package com.example.thigiuaki

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

private val BackgroundLight = Color(0xFFFAF9F6)
private val PrimaryMaroon = Color(0xFF8D021F)
private val SecondaryDark = Color(0xFF424242)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val handleResetPassword: () -> Unit = {
        if (email.isBlank()) {
            errorMessage = "Vui lòng nhập email của bạn"
        } else {
            isLoading = true
            errorMessage = null
            successMessage = null

            auth.sendPasswordResetEmail(email.trim())
                .addOnCompleteListener { task ->
                    isLoading = false
                    if (task.isSuccessful) {
                        successMessage =
                            "Email khôi phục mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư của bạn."
                    } else {
                        errorMessage =
                            task.exception?.localizedMessage ?: "Không thể gửi email khôi phục"
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quên mật khẩu", color = SecondaryDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại", tint = PrimaryMaroon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = SecondaryDark
                )
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                "Khôi phục mật khẩu",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                ),
                color = PrimaryMaroon
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Nhập email của bạn để nhận link khôi phục mật khẩu",
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryDark.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.ArrowBack, contentDescription = "Email Icon", tint = PrimaryMaroon) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
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

            if (successMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryMaroon.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            successMessage!!,
                            color = SecondaryDark
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onSuccess,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryMaroon,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Đã hiểu")
                        }
                    }
                }
            } else {
                Button(
                    onClick = handleResetPassword,
                    enabled = !isLoading && email.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryMaroon,
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Gửi email khôi phục", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }
    }
}
