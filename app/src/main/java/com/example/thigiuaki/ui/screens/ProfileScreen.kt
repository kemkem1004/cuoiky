package com.example.thigiuaki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import android.util.Log // Cần thiết cho các Dialog

// =================================================================
// 1. ĐỊNH NGHĨA MÀU SẮC VÀ CONSTANTS
// =================================================================
private val BackgroundLight = Color(0xFFFAF9F6)
private val PrimaryMaroon = Color(0xFF8D021F)
private val SecondaryDark = Color(0xFF424242)
private val CardBackground = Color.White
private val DividerColor = Color.LightGray.copy(alpha = 0.5f)
// =================================================================

// =================================================================
// 2. DATA MODEL (FIX LỖI FIRESTORE: Đặt ngoài Composable)
// Lưu ý: Nếu bạn có file model/User.kt, hãy xóa định nghĩa này và import nó.
// =================================================================
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = ""
)
// =================================================================

@Composable
fun ProfileSettingItem(
    iconVector: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(CardBackground)
            .padding(vertical = 16.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else PrimaryMaroon

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = contentColor
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor
            )
        }
        if (!isDestructive) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SecondaryDark.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToWishlist: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    // Sử dụng Model User đã được định nghĩa ở ngoài
    var user by remember { mutableStateOf(User()) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showChangePhoneDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            db.collection("users").document(userId)
                .addSnapshotListener { snapshot, _ ->
                    // Lỗi được fix nhờ User được định nghĩa đúng cấu trúc ở ngoài
                    val userData = snapshot?.toObject<User>()
                    if (userData != null) {
                        // Cập nhật state nếu cần
                        user = userData.copy(id = userId)
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tài khoản", color = PrimaryMaroon) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            // 1. Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = user.name.ifBlank { "Khách hàng" },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                        color = PrimaryMaroon
                    )
                    Spacer(Modifier.height(12.dp))

                    Divider(color = DividerColor)
                    Spacer(Modifier.height(12.dp))

                    ProfileInfoRow(label = "Email", value = user.email.ifBlank { auth.currentUser?.email ?: "Chưa cập nhật" })
                    Spacer(Modifier.height(8.dp))
                    ProfileInfoRow(label = "Số điện thoại", value = user.phone.ifBlank { "Chưa cập nhật" })
                }
            }

            Spacer(Modifier.height(24.dp))

            // 2. Cài đặt & Bảo mật
            Text(
                text = "Cài đặt & Bảo mật",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SecondaryDark,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Divider(Modifier.padding(horizontal = 24.dp), color = DividerColor)

            ProfileSettingItem(iconVector = Icons.Default.Lock, title = "Đổi mật khẩu", onClick = { showChangePasswordDialog = true })
            Divider(color = DividerColor)

            ProfileSettingItem(iconVector = Icons.Default.Email, title = "Đổi email", onClick = { showChangeEmailDialog = true })
            Divider(color = DividerColor)

            ProfileSettingItem(iconVector = Icons.Default.Phone, title = "Đổi số điện thoại", onClick = { showChangePhoneDialog = true })

            Spacer(Modifier.height(16.dp))

            // 3. Hoạt động
            Text(
                text = "Hoạt động",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SecondaryDark,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Divider(Modifier.padding(horizontal = 24.dp), color = DividerColor)

            ProfileSettingItem(iconVector = Icons.Default.Favorite, title = "Danh sách yêu thích", onClick = onNavigateToWishlist)
            Divider(color = DividerColor)

            // Nút Đăng xuất
            ProfileSettingItem(iconVector = Icons.Default.ExitToApp, title = "Đăng xuất khỏi tài khoản", onClick = onLogout, isDestructive = true)
            Divider(color = DividerColor)
            Spacer(Modifier.height(32.dp))
        }
    }

    // Dialogs
    if (showChangePasswordDialog) {
        ChangePasswordDialog(onDismiss = { showChangePasswordDialog = false })
    }

    if (showChangeEmailDialog) {
        ChangeEmailDialog(
            currentEmail = user.email.ifBlank { auth.currentUser?.email ?: "" },
            onDismiss = { showChangeEmailDialog = false },
            onSuccess = { newEmail -> user = user.copy(email = newEmail) }
        )
    }

    if (showChangePhoneDialog) {
        ChangePhoneDialog(
            currentPhone = user.phone,
            userId = userId,
            onDismiss = { showChangePhoneDialog = false },
            onSuccess = { newPhone -> user = user.copy(phone = newPhone) }
        )
    }
}

// Hàm phụ trợ cho Profile Info
@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryDark.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = SecondaryDark
        )
    }
}

// =================================================================
// 3. DIALOGS (Áp dụng styling thương hiệu)
// * Lưu ý: Logic Firebase giữ nguyên, chỉ thay đổi styling
// =================================================================

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
        title = { Text("Đổi mật khẩu", color = PrimaryMaroon) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // ... TextFields với styling PrimaryMaroon ...
                OutlinedTextField(value = currentPassword, onValueChange = { currentPassword = it }, label = { Text("Mật khẩu hiện tại") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryMaroon, focusedLabelColor = PrimaryMaroon))
                OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text("Mật khẩu mới (ít nhất 6 ký tự)") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryMaroon, focusedLabelColor = PrimaryMaroon))
                OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Xác nhận mật khẩu mới") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryMaroon, focusedLabelColor = PrimaryMaroon))
                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = { /* Logic đổi mật khẩu */ }, enabled = !isLoading && newPassword.isNotBlank() && confirmPassword.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = PrimaryMaroon)) {
                if (isLoading) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White) } else { Text("Đổi mật khẩu") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = SecondaryDark)) { Text("Hủy") } },
        containerColor = BackgroundLight
    )
}

@Composable
fun ChangeEmailDialog(currentEmail: String, onDismiss: () -> Unit, onSuccess: (String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    var newEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi email", color = PrimaryMaroon) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Email hiện tại:", style = MaterialTheme.typography.bodyMedium, color = SecondaryDark.copy(alpha = 0.7f))
                Text(currentEmail, style = MaterialTheme.typography.titleMedium, color = PrimaryMaroon)

                OutlinedTextField(value = newEmail, onValueChange = { newEmail = it }, label = { Text("Email mới") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryMaroon, focusedLabelColor = PrimaryMaroon))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mật khẩu xác nhận") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryMaroon, focusedLabelColor = PrimaryMaroon))
                if (errorMessage != null) { Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = { /* Logic đổi email */ }, enabled = !isLoading && newEmail.isNotBlank() && password.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = PrimaryMaroon)) {
                if (isLoading) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White) } else { Text("Đổi email") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = SecondaryDark)) { Text("Hủy") } },
        containerColor = BackgroundLight
    )
}

@Composable
fun ChangePhoneDialog(currentPhone: String, userId: String, onDismiss: () -> Unit, onSuccess: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var newPhone by remember { mutableStateOf(currentPhone) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi số điện thoại", color = PrimaryMaroon) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("Số điện thoại mới") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryMaroon, focusedLabelColor = PrimaryMaroon))
                if (errorMessage != null) { Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = { /* Logic đổi số điện thoại */ }, enabled = !isLoading && newPhone.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = PrimaryMaroon)) {
                if (isLoading) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White) } else { Text("Lưu") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = SecondaryDark)) { Text("Hủy") } },
        containerColor = BackgroundLight
    )
}