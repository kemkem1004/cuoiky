package com.example.thigiuaki.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.thigiuaki.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.Timestamp

// ********** THÊM CÁC IMPORTS CẦN THIẾT CHO MATERIAL 3 **********
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.draw.clip

// ***************************************************************

// =================================================================
// 1. ĐỊNH NGHĨA MÀU SẮC (Clean Retail Design)
// =================================================================
private val BackgroundLight = Color(0xFFFAF9F6)
private val PrimaryMaroon = Color(0xFF8D021F)
private val SecondaryDark = Color(0xFF424242)
private val CardBackground = Color.White
private val StatusError = Color(0xFFD32F2F) // Dùng cho Hết hàng/Lỗi
private val ChipSelectedColor = Color(0xFFFDDCDC) // Màu nền nhẹ cho chip được chọn
// =================================================================


@OptIn(ExperimentalMaterial3Api::class)
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
    var sortBy by remember { mutableStateOf("name") }

    // 🔹 Logic Firestore giữ nguyên
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

            // Get new products
            val thirtyDaysAgo = Timestamp.now().toDate().time - (30L * 24 * 60 * 60 * 1000)
            newProducts = list.filter { product ->
                product.createdAt?.toDate()?.time?.let { it >= thirtyDaysAgo } ?: false
            }.sortedByDescending { it.createdAt?.toDate()?.time }.take(10)

            // Get best selling products
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
        }
    }


    // Filter và Sort logic
    val filteredProducts = remember(products, searchQuery, selectedCategory, sortBy) {
        var filtered = products

        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        (it.description?.contains(searchQuery, ignoreCase = true) ?: false) ||
                        (it.category?.contains(searchQuery, ignoreCase = true) ?: false) ||
                        (it.type?.contains(searchQuery, ignoreCase = true) ?: false)
            }
        }

        if (selectedCategory != null) {
            filtered = filtered.filter { it.category == selectedCategory }
        }

        filtered = when (sortBy) {
            "price_asc" -> filtered.sortedBy { it.price }
            "price_desc" -> filtered.sortedByDescending { it.price }
            "name" -> filtered.sortedBy { it.name }
            else -> filtered
        }

        filtered
    }

    val categories = remember(products) {
        products.mapNotNull { it.category }.distinct().filter { it.isNotBlank() }
    }

    // 🔹 UI hiển thị sản phẩm
    Scaffold(
        containerColor = BackgroundLight,
        // Loại bỏ TopBar của Scaffold để sử dụng CustomHeader trong LazyColumn
    ) { paddingValues ->
        // Sử dụng LazyColumn để chứa toàn bộ nội dung, nhưng phần Header sẽ là item cố định
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ===================================================
            // A. HEADER VÀ CÁC THÀNH PHẦN KHÔNG CUỘN (Sticky/Header Look)
            // ===================================================
            item {
                Column(modifier = Modifier.background(CardBackground)) { // CardBackground = White cho Header
                    // Logo + Title


                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Tìm kiếm sản phẩm...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm", tint = SecondaryDark.copy(alpha = 0.6f)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraSmall, // Sử dụng góc bo nhỏ
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryMaroon,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f),
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight,
                            focusedLeadingIconColor = PrimaryMaroon,
                            unfocusedLeadingIconColor = SecondaryDark.copy(alpha = 0.6f),
                            focusedTextColor = SecondaryDark,
                            cursorColor = PrimaryMaroon
                        )
                    )
                }
            }

            // Category Filter
            if (categories.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChipCustom(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = "Tất cả"
                        )
                        categories.forEach { category ->
                            FilterChipCustom(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = category
                            )
                        }
                    }
                }
            }

            // Sort Options
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sắp xếp theo:",
                        modifier = Modifier.padding(end = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = SecondaryDark
                    )
                    // Sử dụng RowScope.FilterChipCustom để căn chỉnh tốt hơn
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChipCustom(selected = sortBy == "name", onClick = { sortBy = "name" }, label = "Tên")
                        FilterChipCustom(selected = sortBy == "price_asc", onClick = { sortBy = "price_asc" }, label = "Giá tăng")
                        FilterChipCustom(selected = sortBy == "price_desc", onClick = { sortBy = "price_desc" }, label = "Giá giảm")
                    }
                }
            }
            // ===================================================

            // B. BEST SELLING PRODUCTS (Bán chạy)
            if (bestSellingProducts.isNotEmpty()) {
                item {
                    Text(
                        "🏆 Sản phẩm bán chạy",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryMaroon,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(bestSellingProducts) { product ->
                            ProductHorizontalItem(product = product, onClick = { onNavigateToProductDetails(product.id) })
                        }
                    }
                }
            }

            // C. NEW PRODUCTS (Sản phẩm mới)
            if (newProducts.isNotEmpty()) {
                item {
                    Text(
                        "🔥 Sản phẩm mới",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryMaroon,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(newProducts) { product ->
                            ProductHorizontalItem(product = product, onClick = { onNavigateToProductDetails(product.id) })
                        }
                    }
                }
            }

            // D. ALL PRODUCTS (Tất cả sản phẩm)
            item {
                Text(
                    "🛒 Tất cả sản phẩm (${filteredProducts.size})",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SecondaryDark,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(filteredProducts) { p ->
                ProductCustomerItem(product = p, onClick = { onNavigateToProductDetails(p.id) }, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

// =================================================================
// 2. CÁC THÀNH PHẦN PHỤ TRỢ (Đã tùy chỉnh giao diện)
// =================================================================

// Thêm Modifier vào ProductCustomerItem
@Composable
fun ProductCustomerItem(
    product: Product,
    modifier: Modifier = Modifier, // Thêm modifier
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (product.imageUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(product.imageUrl),
                    contentDescription = product.name,
                    // Sử dụng clip và fill để hình ảnh hiển thị đẹp hơn
                    modifier = Modifier.size(90.dp).background(Color.LightGray).clip(MaterialTheme.shapes.small)
                )
                Spacer(Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = SecondaryDark, maxLines = 2)
                Spacer(Modifier.height(4.dp))

                // Hiển thị giá
                Text(
                    text = "${product.price.toInt()} VND",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
                    color = PrimaryMaroon
                )
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    // Trạng thái tồn kho
                    if (product.stock > 0) {
                        Text(text = "Còn: ${product.stock}", style = MaterialTheme.typography.bodySmall, color = if (product.stock < 10) StatusError else SecondaryDark.copy(alpha = 0.7f))
                    } else {
                        Text(text = "Hết hàng", style = MaterialTheme.typography.bodySmall, color = StatusError, fontWeight = FontWeight.Bold)
                    }

                    // Rating
                    if ((product.rating ?: 0.0) > 0) {
                        Spacer(Modifier.width(12.dp))
                        Text(text = "⭐ ${String.format("%.1f", product.rating)}", style = MaterialTheme.typography.bodySmall, color = SecondaryDark)
                    }
                }
            }
        }
    }
}

// Giữ nguyên ProductHorizontalItem

@Composable
fun ProductHorizontalItem(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.width(170.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (product.imageUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(product.imageUrl),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxWidth().height(140.dp).background(Color.LightGray)
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(text = product.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = SecondaryDark, maxLines = 2, minLines = 2)
            Spacer(Modifier.height(4.dp))
            Text(text = "${product.price.toInt()} VND", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = PrimaryMaroon)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.FilterChipCustom(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)) },
        // Tùy chỉnh màu sắc chip

        border = if (selected) null else BorderStroke(1.dp, Color.LightGray)
    )
}