package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.thigiuaki.model.Review
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReviewManagementScreen() {
    val db = FirebaseFirestore.getInstance()
    var reviews by remember { mutableStateOf(listOf<Review>()) }
    var isLoading by remember { mutableStateOf(true) }

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
                title = { Text("Quản lý đánh giá") }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            reviews.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chưa có đánh giá nào")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ===== User info =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    review.userName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "⭐".repeat(review.rating),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                review.comment,
                style = MaterialTheme.typography.bodyMedium
            )

            // ===== ADMIN REPLY =====
            if (!review.adminReply.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                Text(
                    "Phản hồi của admin:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    review.adminReply,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(12.dp))

            // ===== BUTTON =====
            Button(
                onClick = { showReplyDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (review.adminReply.isNullOrBlank())
                        "Trả lời đánh giá"
                    else
                        "Sửa phản hồi"
                )
            }
        }
    }

    // ===== DIALOG TRẢ LỜI / SỬA =====
    if (showReplyDialog) {
        AlertDialog(
            onDismissRequest = { showReplyDialog = false },
            title = {
                Text(
                    if (review.adminReply.isNullOrBlank())
                        "Trả lời đánh giá"
                    else
                        "Sửa phản hồi"
                )
            },
            text = {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text("Nội dung phản hồi") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )
            },
            confirmButton = {
                Button(
                    onClick = {
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
                    },
                    enabled = replyText.isNotBlank()
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplyDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

