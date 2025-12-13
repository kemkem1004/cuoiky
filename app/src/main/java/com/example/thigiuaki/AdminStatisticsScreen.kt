package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStatisticsScreen() {
    val db = FirebaseFirestore.getInstance()
    var dailyRevenue by remember { mutableStateOf(0.0) }
    var monthlyRevenue by remember { mutableStateOf(0.0) }
    var totalOrders by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = com.google.firebase.Timestamp(today.time)
        
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStartTimestamp = com.google.firebase.Timestamp(monthStart.time)
        
        // Daily revenue
        db.collection("orders")
            .whereGreaterThanOrEqualTo("createdAt", todayStart)
            .whereEqualTo("status", "delivered")
            .get()
            .addOnSuccessListener { snapshot ->
                dailyRevenue = snapshot.documents.sumOf { doc ->
                    (doc.getDouble("totalAmount") ?: 0.0)
                }
            }
        
        // Monthly revenue
        db.collection("orders")
            .whereGreaterThanOrEqualTo("createdAt", monthStartTimestamp)
            .whereEqualTo("status", "delivered")
            .get()
            .addOnSuccessListener { snapshot ->
                monthlyRevenue = snapshot.documents.sumOf { doc ->
                    (doc.getDouble("totalAmount") ?: 0.0)
                }
            }
        
        // Total orders
        db.collection("orders")
            .get()
            .addOnSuccessListener { snapshot ->
                totalOrders = snapshot.size()
                isLoading = false
            }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thống kê doanh thu") }
            )
        }
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
                    CircularProgressIndicator()
                }
            } else {
                StatisticCard(
                    title = "Doanh thu hôm nay",
                    value = "${dailyRevenue.toInt()} VND",
                    icon = Icons.Default.Today
                )
                
                StatisticCard(
                    title = "Doanh thu tháng này",
                    value = "${monthlyRevenue.toInt()} VND",
                    icon = Icons.Default.CalendarMonth
                )
                
                StatisticCard(
                    title = "Tổng số đơn hàng",
                    value = "$totalOrders đơn",
                    icon = Icons.Default.ShoppingCart
                )
            }
        }
    }
}

@Composable
fun StatisticCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

