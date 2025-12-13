package com.example.thigiuaki

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

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
                        successMessage = "Email khôi phục mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư của bạn."
                    } else {
                        errorMessage = "Lỗi: ${task.exception?.localizedMessage ?: "Không thể gửi email khôi phục"}"
                    }
                }
        }


        isLoading = true
        errorMessage = null
        successMessage = null

        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    successMessage = "Email khôi phục mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư của bạn."
                } else {
                    errorMessage = "Lỗi: ${task.exception?.localizedMessage ?: "Không thể gửi email khôi phục"}"
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quên mật khẩu") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Khôi phục mật khẩu",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Nhập email của bạn để nhận link khôi phục mật khẩu",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
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
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            successMessage!!,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onSuccess,
                            modifier = Modifier.fillMaxWidth()
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
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Gửi email khôi phục")
                    }
                }
            }
        }
    }
}

