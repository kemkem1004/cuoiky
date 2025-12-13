package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.thigiuaki.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomerManagementScreen() {
    val db = FirebaseFirestore.getInstance()
    var customers by remember { mutableStateOf(listOf<User>()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("role", "user")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AdminCustomers", "Error: ${error.message}")
                    isLoading = false
                    return@addSnapshotListener
                }
                
                val customerList = snapshot?.documents?.mapNotNull { doc ->
                    val user = doc.toObject<User>()
                    user?.copy(id = doc.id)
                } ?: emptyList()
                
                customers = customerList
                isLoading = false
            }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý khách hàng") }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (customers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Không có khách hàng nào")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(customers) { customer ->
                    AdminCustomerCard(
                        customer = customer,
                        onBlock = {
                            db.collection("users").document(customer.id)
                                .update("isBlocked", true)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminCustomerCard(
    customer: User,
    onBlock: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var orderCount by remember { mutableStateOf(0) }
    var showBlockDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(customer.id) {
        db.collection("orders")
            .whereEqualTo("userId", customer.id)
            .get()
            .addOnSuccessListener { snapshot ->
                orderCount = snapshot.size()
            }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        customer.name.ifBlank { "Chưa có tên" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        customer.email,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "SĐT: ${customer.phone.ifBlank { "Chưa cập nhật" }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Số đơn hàng: $orderCount",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(
                    onClick = { showBlockDialog = true }
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = "Chặn",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Chặn khách hàng") },
            text = { Text("Bạn có chắc chắn muốn chặn khách hàng này?") },
            confirmButton = {
                Button(
                    onClick = {
                        onBlock()
                        showBlockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Chặn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

