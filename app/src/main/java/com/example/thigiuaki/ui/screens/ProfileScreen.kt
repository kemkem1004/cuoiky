package com.example.thigiuaki.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

@Composable
fun ProfileSettingItem(
    iconVector: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null
        )
    }
}

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToWishlist: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    
    var user by remember { mutableStateOf(com.example.thigiuaki.model.User()) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showChangePhoneDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            db.collection("users").document(userId)
                .addSnapshotListener { snapshot, _ ->
                    val userData = snapshot?.toObject<com.example.thigiuaki.model.User>()
                    if (userData != null) {
                        user = userData.copy(id = userId)
                    }
                }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Hồ sơ cá nhân",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = user.name.ifBlank { "Chưa cập nhật" },
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Email: ${user.email.ifBlank { auth.currentUser?.email ?: "Chưa cập nhật" }}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Số điện thoại: ${user.phone.ifBlank { "Chưa cập nhật" }}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Divider(Modifier.padding(horizontal = 16.dp))

        ProfileSettingItem(
            iconVector = Icons.Default.Lock,
            title = "Đổi mật khẩu",
            onClick = { showChangePasswordDialog = true }
        )
        Divider(Modifier.padding(horizontal = 16.dp))
        
        ProfileSettingItem(
            iconVector = Icons.Default.Email,
            title = "Đổi email",
            onClick = { showChangeEmailDialog = true }
        )
        Divider(Modifier.padding(horizontal = 16.dp))
        
        ProfileSettingItem(
            iconVector = Icons.Default.Phone,
            title = "Đổi số điện thoại",
            onClick = { showChangePhoneDialog = true }
        )
        Divider(Modifier.padding(horizontal = 16.dp))

        ProfileSettingItem(
            iconVector = Icons.Default.Favorite,
            title = "Danh sách yêu thích",
            onClick = onNavigateToWishlist
        )
        Divider(Modifier.padding(horizontal = 16.dp))

        ProfileSettingItem(
            iconVector = Icons.Default.ExitToApp,
            title = "Đăng xuất",
            onClick = onLogout
        )
    }
    
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false }
        )
    }
    
    if (showChangeEmailDialog) {
        ChangeEmailDialog(
            currentEmail = user.email.ifBlank { auth.currentUser?.email ?: "" },
            onDismiss = { showChangeEmailDialog = false },
            onSuccess = { newEmail ->
                user = user.copy(email = newEmail)
            }
        )
    }
    
    if (showChangePhoneDialog) {
        ChangePhoneDialog(
            currentPhone = user.phone,
            userId = userId,
            onDismiss = { showChangePhoneDialog = false },
            onSuccess = { newPhone ->
                user = user.copy(phone = newPhone)
            }
        )
    }
}

@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi mật khẩu") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Mật khẩu hiện tại") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Mật khẩu mới") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Xác nhận mật khẩu mới") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword != confirmPassword) {
                        errorMessage = "Mật khẩu xác nhận không khớp"
                        return@Button
                    }
                    if (newPassword.length < 6) {
                        errorMessage = "Mật khẩu phải có ít nhất 6 ký tự"
                        return@Button
                    }
                    isLoading = true
                    val user = auth.currentUser
                    if (user != null && user.email != null) {
                        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(
                            user.email!!, currentPassword
                        )
                        user.reauthenticate(credential)
                            .addOnSuccessListener {
                                user.updatePassword(newPassword)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        onDismiss()
                                    }
                                    .addOnFailureListener { e ->
                                        errorMessage = "Lỗi: ${e.localizedMessage}"
                                        isLoading = false
                                    }
                            }
                            .addOnFailureListener { e ->
                                errorMessage = "Mật khẩu hiện tại không đúng"
                                isLoading = false
                            }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Đổi mật khẩu")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun ChangeEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    var newEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi email") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Email hiện tại: $currentEmail")
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("Email mới") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu xác nhận") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    val user = auth.currentUser
                    if (user != null && user.email != null) {
                        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(
                            user.email!!, password
                        )
                        user.reauthenticate(credential)
                            .addOnSuccessListener {
                                user.updateEmail(newEmail)
                                    .addOnSuccessListener {
                                        db.collection("users").document(userId)
                                            .update("email", newEmail)
                                            .addOnSuccessListener {
                                                isLoading = false
                                                onSuccess(newEmail)
                                                onDismiss()
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        errorMessage = "Lỗi: ${e.localizedMessage}"
                                        isLoading = false
                                    }
                            }
                            .addOnFailureListener { e ->
                                errorMessage = "Mật khẩu không đúng"
                                isLoading = false
                            }
                    }
                },
                enabled = !isLoading && newEmail.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Đổi email")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun ChangePhoneDialog(
    currentPhone: String,
    userId: String,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var newPhone by remember { mutableStateOf(currentPhone) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi số điện thoại") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newPhone,
                    onValueChange = { newPhone = it },
                    label = { Text("Số điện thoại mới") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPhone.isBlank()) {
                        errorMessage = "Vui lòng nhập số điện thoại"
                        return@Button
                    }
                    isLoading = true
                    db.collection("users").document(userId)
                        .update("phone", newPhone)
                        .addOnSuccessListener {
                            isLoading = false
                            onSuccess(newPhone)
                            onDismiss()
                        }
                        .addOnFailureListener { e ->
                            errorMessage = "Lỗi: ${e.localizedMessage}"
                            isLoading = false
                        }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Lưu")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

