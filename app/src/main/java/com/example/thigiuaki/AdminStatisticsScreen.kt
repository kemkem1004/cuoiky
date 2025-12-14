package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

// =================================================================
// 1. ĐỊNH NGHĨA MÀU SẮC (Clean Retail Design)
// =================================================================
private val BackgroundLight = Color(0xFFFAF9F6)
private val PrimaryMaroon = Color(0xFF8D021F)
private val SecondaryDark = Color(0xFF424242)
private val CardBackground = Color.White
private val AccentGreen = Color(0xFF388E3C) // Dùng cho biểu tượng Doanh thu
private val AccentBlue = Color(0xFF1976D2)  // Dùng cho biểu tượng Đơn hàng
// =================================================================


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStatisticsScreen() {
    val db = FirebaseFirestore.getInstance()
    var dailyRevenue by remember { mutableStateOf(0.0) }
    var monthlyRevenue by remember { mutableStateOf(0.0) }
    var totalOrders by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // --- 1. Thiết lập khoảng thời gian (Giữ nguyên) ---
        val today = Calendar.getInstance().apply {
            time = Date()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = Timestamp(today.time)

        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStartTimestamp = Timestamp(monthStart.time)

        // --- 2. Daily revenue (Logic giữ nguyên) ---
        db.collection("orders")
            .whereGreaterThanOrEqualTo("createdAt", todayStart)
            .whereEqualTo("status", "delivered")
            .get()
            .addOnSuccessListener { snapshot ->
                dailyRevenue = snapshot.documents.sumOf { doc ->
                    doc.getDouble("totalAmount") ?: 0.0
                }
            }
            .addOnFailureListener { e ->
                Log.e("AdminStats", "Error fetching daily revenue (CHECK INDEXES): ${e.message}")
            }

        // --- 3. Monthly revenue (Logic giữ nguyên) ---
        db.collection("orders")
            .whereGreaterThanOrEqualTo("createdAt", monthStartTimestamp)
            .whereEqualTo("status", "delivered")
            .get()
            .addOnSuccessListener { snapshot ->
                monthlyRevenue = snapshot.documents.sumOf { doc ->
                    doc.getDouble("totalAmount") ?: 0.0
                }
            }
            .addOnFailureListener { e ->
                Log.e("AdminStats", "Error fetching monthly revenue (CHECK INDEXES): ${e.message}")
            }

        // --- 4. Total orders (Logic giữ nguyên) ---
        db.collection("orders")
            .get()
            .addOnSuccessListener { snapshot ->
                totalOrders = snapshot.size()
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("AdminStats", "Error fetching total orders: ${e.message}")
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thống kê doanh thu", color = PrimaryMaroon) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryMaroon)
                }
            } else {
                StatisticCard(
                    title = "Doanh thu hôm nay",
                    value = "${dailyRevenue.toLong()} VND",
                    icon = Icons.Default.Today,
                    tintColor = AccentGreen
                )

                StatisticCard(
                    title = "Doanh thu tháng này",
                    value = "${monthlyRevenue.toLong()} VND",
                    icon = Icons.Default.CalendarMonth,
                    tintColor = PrimaryMaroon
                )

                StatisticCard(
                    title = "Tổng số đơn hàng",
                    value = "$totalOrders đơn",
                    icon = Icons.Default.ShoppingCart,
                    tintColor = AccentBlue
                )
            }
        }
    }
}

@Composable
fun StatisticCard(
    title: String,
    value: String,
    icon: ImageVector,
    tintColor: Color // Thêm tham số màu sắc cho icon và giá trị
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = SecondaryDark.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = tintColor // Sử dụng màu nhấn động
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp), // Icon lớn hơn
                tint = tintColor.copy(alpha = 0.8f) // Icon lớn và nổi bật hơn
            )
        }
    }
}