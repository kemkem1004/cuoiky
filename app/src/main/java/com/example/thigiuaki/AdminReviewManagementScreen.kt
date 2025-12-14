package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thigiuaki.model.Review
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

// =================================================================
// 1. ĐỊNH NGHĨA MÀU SẮC (Clean Retail Design)
// =================================================================
private val BackgroundLight = Color(0xFFFAF9F6)
private val PrimaryMaroon = Color(0xFF8D021F)
private val SecondaryDark = Color(0xFF424242)
private val CardBackground = Color.White
private val GoldStar = Color(0xFFFFC72C)
// =================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReviewManagementScreen() {
    val db = FirebaseFirestore.getInstance()
    var reviews by remember { mutableStateOf(listOf<Review>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Logic tải dữ liệu (Giữ nguyên)
    LaunchedEffect(Unit) {
        db.collection("reviews")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AdminReviews", "Error: ${error.message}")
                    isLoading = false
                    return@addSnapshotListener
                }

                val reviewList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Review>()?.copy(id = doc.id)
                } ?: emptyList()

                reviews = reviewList
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý đánh giá", color = PrimaryMaroon) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryMaroon)
                }
            }

            reviews.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Chưa có đánh giá nào",
                        style = MaterialTheme.typography.titleMedium,
                        color = SecondaryDark
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(reviews) { review ->
                        AdminReviewCard(review = review)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminReviewCard(review: Review) {
    val db = FirebaseFirestore.getInstance()

    var showReplyDialog by remember { mutableStateOf(false) }

    // 👉 Nếu đã có adminReply thì load sẵn để sửa
    var replyText by remember {
        mutableStateOf(review.adminReply ?: "")
    }

    // Hàm xử lý lưu/cập nhật phản hồi (Giữ nguyên logic Firebase)
    val handleSaveReply: () -> Unit = {
        db.collection("reviews")
            .document(review.id)
            .update(
                mapOf(
                    "adminReply" to replyText,
                    "repliedAt" to Timestamp.now()
                )
            )
            .addOnSuccessListener {
                showReplyDialog = false
            }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ===== User info & Rating =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = SecondaryDark.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        review.userName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SecondaryDark
                    )
                }

                // Rating Stars
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "⭐".repeat(review.rating),
                        style = MaterialTheme.typography.titleLarge.copy(color = GoldStar)
                    )
                    Text(" (${review.rating}/5)", color = SecondaryDark.copy(alpha = 0.6f))
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            // ===== REVIEW COMMENT =====
            Text(
                "Đánh giá:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = SecondaryDark.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                review.comment,
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryDark
            )

            // ===== ADMIN REPLY =====
            if (!review.adminReply.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Divider(color = PrimaryMaroon.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))

                Text(
                    "Phản hồi của admin:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryMaroon
                )

                Spacer(Modifier.height(4.dp))

                Surface(
                    color = PrimaryMaroon.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, PrimaryMaroon.copy(alpha = 0.2f))
                ) {
                    Text(
                        review.adminReply!!,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryDark
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ===== BUTTON =====
            Button(
                onClick = { showReplyDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (review.adminReply.isNullOrBlank()) PrimaryMaroon else SecondaryDark.copy(alpha = 0.8f)
                )
            ) {
                Icon(
                    imageVector = if (review.adminReply.isNullOrBlank()) Icons.Default.Reply else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (review.adminReply.isNullOrBlank())
                        "TRẢ LỜI ĐÁNH GIÁ"
                    else
                        "SỬA PHẢN HỒI"
                )
            }
        }
    }

    // ===== DIALOG TRẢ LỜI / SỬA (Đã tùy chỉnh giao diện) =====
    if (showReplyDialog) {
        AlertDialog(
            onDismissRequest = { showReplyDialog = false },
            title = {
                Text(
                    if (review.adminReply.isNullOrBlank())
                        "Trả lời đánh giá"
                    else
                        "Sửa phản hồi"
                    , color = PrimaryMaroon
                )
            },
            text = {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text("Nội dung phản hồi") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryMaroon,
                        focusedLabelColor = PrimaryMaroon,
                        cursorColor = PrimaryMaroon
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = handleSaveReply,
                    enabled = replyText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMaroon)
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = SecondaryDark)) {
                    Text("Hủy")
                }
            },
            containerColor = CardBackground
        )
    }
}