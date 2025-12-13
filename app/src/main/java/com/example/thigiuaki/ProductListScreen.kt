package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.thigiuaki.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

@Composable
fun ProductListScreen(
    onLogout: () -> Unit,
    onNavigateToProductDetails: (String) -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()
    var products by remember { mutableStateOf(listOf<Product>()) }
    var newProducts by remember { mutableStateOf(listOf<Product>()) }
    var bestSellingProducts by remember { mutableStateOf(listOf<Product>()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf("name") } // name, price_asc, price_desc

    // 🔹 Nghe realtime thay đổi từ Firestore
    LaunchedEffect(Unit) {
        db.collection("products").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "❌ Lỗi: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot == null || snapshot.isEmpty) {
                Log.w("Firestore", "⚠️ Không có sản phẩm nào trong Firestore.")
                products = emptyList()
                return@addSnapshotListener
            }

            val list = snapshot.documents.mapNotNull { doc ->
                val data = doc.toObject<Product>()
                data?.copy(id = doc.id)
            }

            products = list
            
            // Get new products (created within last 30 days)
            val thirtyDaysAgo = com.google.firebase.Timestamp.now().toDate().time - (30L * 24 * 60 * 60 * 1000)
            newProducts = list.filter { product ->
                product.createdAt?.toDate()?.time?.let { it >= thirtyDaysAgo } ?: false
            }.sortedByDescending { it.createdAt?.toDate()?.time }.take(10)
            
            // Get best selling products from orders
            db.collection("orders")
                .whereEqualTo("status", "delivered")
                .get()
                .addOnSuccessListener { ordersSnapshot ->
                    val productSales = mutableMapOf<String, Int>()
                    ordersSnapshot.documents.forEach { orderDoc ->
                        val items = orderDoc.get("items") as? List<Map<String, Any>> ?: emptyList()
                        items.forEach { item ->
                            val productId = item["productId"] as? String ?: ""
                            val quantity = (item["quantity"] as? Long)?.toInt() ?: 0
                            productSales[productId] = (productSales[productId] ?: 0) + quantity
                        }
                    }
                    val sortedProductIds = productSales.toList().sortedByDescending { it.second }.map { it.first }
                    bestSellingProducts = sortedProductIds.mapNotNull { productId ->
                        products.find { it.id == productId }
                    }.take(10)
                }
            
            Log.d("Firestore", "✅ Lấy được ${list.size} sản phẩm.")
        }
    }

    // Filter and sort products
    val filteredProducts = remember(products, searchQuery, selectedCategory, sortBy) {
        var filtered = products
        
        // Filter by search query
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.type.contains(searchQuery, ignoreCase = true)
            }
        }
        
        // Filter by category
        if (selectedCategory != null) {
            filtered = filtered.filter { it.category == selectedCategory }
        }
        
        // Sort
        filtered = when (sortBy) {
            "price_asc" -> filtered.sortedBy { it.price }
            "price_desc" -> filtered.sortedByDescending { it.price }
            "name" -> filtered.sortedBy { it.name }
            else -> filtered
        }
        
        filtered
    }

    val categories = remember(products) {
        products.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    // 🔹 UI hiển thị sản phẩm
    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cửa hàng quần áo", style = MaterialTheme.typography.headlineSmall)
                }
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Tìm kiếm sản phẩm...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm") },
                    singleLine = true
                )
                
                // Category Filter
                if (categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("Tất cả") }
                        )
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) }
                            )
                        }
                    }
                }
                
                // Sort Options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Sắp xếp:", modifier = Modifier.padding(end = 8.dp))
                    FilterChip(
                        selected = sortBy == "name",
                        onClick = { sortBy = "name" },
                        label = { Text("Tên") }
                    )
                    FilterChip(
                        selected = sortBy == "price_asc",
                        onClick = { sortBy = "price_asc" },
                        label = { Text("Giá tăng") }
                    )
                    FilterChip(
                        selected = sortBy == "price_desc",
                        onClick = { sortBy = "price_desc" },
                        label = { Text("Giá giảm") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (products.isEmpty()) "Không có sản phẩm nào được hiển thị." else "Không tìm thấy sản phẩm phù hợp.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // New Products Section
                    if (newProducts.isNotEmpty()) {
                        item {
                            Text(
                                "Sản phẩm mới",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(newProducts) { product ->
                                    ProductHorizontalItem(
                                        product = product,
                                        onClick = { onNavigateToProductDetails(product.id) }
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                    
                    // Best Selling Products Section
                    if (bestSellingProducts.isNotEmpty()) {
                        item {
                            Text(
                                "Sản phẩm bán chạy",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(bestSellingProducts) { product ->
                                    ProductHorizontalItem(
                                        product = product,
                                        onClick = { onNavigateToProductDetails(product.id) }
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                    
                    // All Products Section
                    item {
                        Text(
                            "Tất cả sản phẩm",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(filteredProducts) { p ->
                        ProductCustomerItem(
                            product = p,
                            onClick = { onNavigateToProductDetails(p.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCustomerItem(
    product: Product,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 🔸 Hiển thị ảnh sản phẩm nếu có
            if (product.imageUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(product.imageUrl),
                    contentDescription = "Ảnh sản phẩm",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${product.price.toInt()} VND",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (product.rating > 0) {
                    Text(
                        text = "⭐ ${String.format("%.1f", product.rating)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            if (product.category.isNotBlank()) {
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (product.type.isNotBlank()) {
                Text(
                    text = "Loại: ${product.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (product.stock > 0) {
                Text(
                    text = "Còn lại: ${product.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.stock < 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                )
            } else {
                Text(
                    text = "Hết hàng",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ProductHorizontalItem(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            if (product.imageUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(product.imageUrl),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            Text(
                text = "${product.price.toInt()} VND",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
