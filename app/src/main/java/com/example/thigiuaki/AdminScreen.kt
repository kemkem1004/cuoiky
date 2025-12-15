package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.thigiuaki.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

// =================================================================
// Color Theme giống ProductDetailsScreen
// =================================================================
private val BackgroundLight = Color(0xFFFAF9F6)
private val PrimaryMaroon = Color(0xFF8D021F)
private val SecondaryDark = Color(0xFF424242)
private val CardBackground = Color.White
private val StatusError = Color(0xFFD32F2F)
// =================================================================

sealed class AdminScreenType {
    object Products : AdminScreenType()
    object Orders : AdminScreenType()
    object Statistics : AdminScreenType()
    object Customers : AdminScreenType()
    object Reviews : AdminScreenType()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onLogout: () -> Unit
) {
    var currentScreen by remember { mutableStateOf<AdminScreenType>(AdminScreenType.Products) }
    val db = FirebaseFirestore.getInstance()

    // Trạng thái nhập liệu
    var newName by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    var newStock by remember { mutableStateOf("") }
    var newSizes by remember { mutableStateOf("S,M,L,XL") }
    var newColors by remember { mutableStateOf("Black,White,Blue") }
    var newImageUrl by remember { mutableStateOf("") }
    var selectedImageLabel by remember { mutableStateOf("Ảnh mặc định 1") }
    var products by remember { mutableStateOf(listOf<Product>()) }
    var expanded by remember { mutableStateOf(false) }

    // Trạng thái chỉnh sửa
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    // Danh sách ảnh mẫu có sẵn
    val imageOptions = listOf(
        "Ảnh mặc định 1" to "https://img.icons8.com/ios-filled/200/8D021F/camera.png",
        "Ảnh mặc định 2" to "https://img.icons8.com/ios-filled/200/gallery.png",
        "Ảnh mặc định 3" to "https://img.icons8.com/ios-filled/200/compass.png"
    )
    var selectedImageUrl by remember { mutableStateOf(imageOptions.first().second) }

    // Lắng nghe Firestore
    LaunchedEffect(Unit) {
        db.collection("products").addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { doc ->
                val p = doc.toObject<Product>()
                p?.copy(id = doc.id)
            } ?: emptyList()
            products = list
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trang quản lý", color = PrimaryMaroon) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Đăng xuất", tint = PrimaryMaroon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = BackgroundLight) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Sản phẩm") },
                    label = { Text("Sản phẩm") },
                    selected = currentScreen == AdminScreenType.Products,
                    onClick = { currentScreen = AdminScreenType.Products },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryMaroon,
                        selectedTextColor = PrimaryMaroon,
                        unselectedIconColor = SecondaryDark,
                        unselectedTextColor = SecondaryDark
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Description, contentDescription = "Đơn hàng") },
                    label = { Text("Đơn hàng") },
                    selected = currentScreen == AdminScreenType.Orders,
                    onClick = { currentScreen = AdminScreenType.Orders }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Thống kê") },
                    label = { Text("Thống kê") },
                    selected = currentScreen == AdminScreenType.Statistics,
                    onClick = { currentScreen = AdminScreenType.Statistics }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.People, contentDescription = "Khách hàng") },
                    label = { Text("Khách hàng") },
                    selected = currentScreen == AdminScreenType.Customers,
                    onClick = { currentScreen = AdminScreenType.Customers }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = "Đánh giá") },
                    label = { Text("Đánh giá") },
                    selected = currentScreen == AdminScreenType.Reviews,
                    onClick = { currentScreen = AdminScreenType.Reviews }
                )
            }
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
            when (currentScreen) {
                is AdminScreenType.Products -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // ----- Card Form nhập liệu -----
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                                Text(
                                    if (editingProduct != null) "✏️ Chỉnh sửa sản phẩm:" else "➕ Thêm sản phẩm mới:",
                                    style = MaterialTheme.typography.titleMedium.copy(color = PrimaryMaroon)
                                )

                                OutlinedTextField(
                                    value = newName,
                                    onValueChange = { newName = it },
                                    label = { Text("Tên sản phẩm") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newPrice,
                                    onValueChange = { newPrice = it },
                                    label = { Text("Giá") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Dropdown ảnh
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedImageLabel,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Chọn ảnh mặc định") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expanded = true }
                                    )
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        imageOptions.forEach { (label, url) ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    selectedImageLabel = label
                                                    selectedImageUrl = url
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = newImageUrl,
                                    onValueChange = { newImageUrl = it },
                                    label = { Text("Hoặc nhập URL ảnh Internet (tùy chọn)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val previewImageUrl = if (newImageUrl.isNotBlank()) newImageUrl else selectedImageUrl
                                Image(
                                    painter = rememberAsyncImagePainter(previewImageUrl),
                                    contentDescription = "Ảnh xem trước",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(BackgroundLight)
                                )

                                OutlinedTextField(
                                    value = newType,
                                    onValueChange = { newType = it },
                                    label = { Text("Loại sản phẩm") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Category dropdown
                                var categoryExpanded by remember { mutableStateOf(false) }
                                val categories = listOf("Men", "Women", "Kids", "Accessories")
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = newCategory,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Danh mục") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { categoryExpanded = true }
                                    )
                                    DropdownMenu(
                                        expanded = categoryExpanded,
                                        onDismissRequest = { categoryExpanded = false }
                                    ) {
                                        categories.forEach { category ->
                                            DropdownMenuItem(
                                                text = { Text(category) },
                                                onClick = {
                                                    newCategory = category
                                                    categoryExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = newDescription,
                                    onValueChange = { newDescription = it },
                                    label = { Text("Mô tả") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )
                                OutlinedTextField(
                                    value = newSizes,
                                    onValueChange = { newSizes = it },
                                    label = { Text("Kích thước (S,M,L,XL)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newColors,
                                    onValueChange = { newColors = it },
                                    label = { Text("Màu sắc (Black,White,Blue)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newStock,
                                    onValueChange = { newStock = it },
                                    label = { Text("Số lượng tồn kho") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(8.dp))

                                // Nút thêm / cập nhật
                                Button(
                                    onClick = {
                                        val priceValue = newPrice.toDoubleOrNull() ?: 0.0
                                        val stockValue = newStock.toIntOrNull() ?: 0
                                        if (newName.isBlank() || priceValue <= 0) return@Button

                                        val imageUrlToSave =
                                            if (newImageUrl.isNotBlank()) newImageUrl else selectedImageUrl

                                        val sizesList = newSizes.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                        val colorsList = newColors.split(",").map { it.trim() }.filter { it.isNotBlank() }

                                        val productToSave = Product(
                                            id = editingProduct?.id ?: "",
                                            name = newName.trim(),
                                            price = priceValue,
                                            type = newType.trim(),
                                            category = newCategory.trim(),
                                            imageUrl = imageUrlToSave,
                                            description = newDescription.trim(),
                                            sizes = if (sizesList.isEmpty()) listOf("S", "M", "L", "XL") else sizesList,
                                            colors = if (colorsList.isEmpty()) listOf("Black", "White", "Blue") else colorsList,
                                            stock = stockValue
                                        )

                                        if (editingProduct != null) {
                                            db.collection("products")
                                                .document(editingProduct!!.id)
                                                .set(productToSave)
                                                .addOnSuccessListener {
                                                    editingProduct = null
                                                    newName = ""
                                                    newPrice = ""
                                                    newType = ""
                                                    newCategory = ""
                                                    newDescription = ""
                                                    newSizes = "S,M,L,XL"
                                                    newColors = "Black,White,Blue"
                                                    newStock = ""
                                                    newImageUrl = ""
                                                    selectedImageLabel = "Ảnh mặc định 1"
                                                }
                                        } else {
                                            db.collection("products")
                                                .add(productToSave)
                                                .addOnSuccessListener {
                                                    newName = ""
                                                    newPrice = ""
                                                    newType = ""
                                                    newCategory = ""
                                                    newDescription = ""
                                                    newSizes = "S,M,L,XL"
                                                    newColors = "Black,White,Blue"
                                                    newStock = ""
                                                    newImageUrl = ""
                                                    selectedImageLabel = "Ảnh mặc định 1"
                                                }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMaroon)
                                ) {
                                    Text(if (editingProduct != null) "CẬP NHẬT SẢN PHẨM" else "THÊM SẢN PHẨM", color = Color.White)
                                }

                                if (editingProduct != null) {
                                    OutlinedButton(
                                        onClick = {
                                            editingProduct = null
                                            newName = ""
                                            newPrice = ""
                                            newType = ""
                                            newCategory = ""
                                            newDescription = ""
                                            newSizes = "S,M,L,XL"
                                            newColors = "Black,White,Blue"
                                            newStock = ""
                                            newImageUrl = ""
                                            selectedImageLabel = "Ảnh mặc định 1"
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
                                    ) {
                                        Text("HỦY CHỈNH SỬA")
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // ----- Danh sách sản phẩm -----
                        Text("Danh sách sản phẩm:", style = MaterialTheme.typography.titleMedium, color = PrimaryMaroon)
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            products.forEach { p ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                                    elevation = CardDefaults.cardElevation(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ProductAdminItem(
                                        product = p,
                                        onEditClicked = { prod ->
                                            editingProduct = prod
                                            newName = prod.name
                                            newPrice = prod.price.toString()
                                            newType = prod.type
                                            newCategory = prod.category
                                            newDescription = prod.description
                                            newSizes = prod.sizes.joinToString(",")
                                            newColors = prod.colors.joinToString(",")
                                            newStock = prod.stock.toString()
                                            newImageUrl = prod.imageUrl
                                            selectedImageLabel = "Ảnh mặc định 1"
                                        },
                                        onDeleteClicked = { prod ->
                                            if (prod.id.isNotBlank()) {
                                                db.collection("products").document(prod.id).delete()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                is AdminScreenType.Orders -> AdminOrderManagementScreen()
                is AdminScreenType.Statistics -> AdminStatisticsScreen()
                is AdminScreenType.Customers -> AdminCustomerManagementScreen()
                is AdminScreenType.Reviews -> AdminReviewManagementScreen()
            }
        }
    }
}

@Composable
fun ProductAdminItem(
    product: Product,
    onEditClicked: (Product) -> Unit,
    onDeleteClicked: (Product) -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ----- Ảnh sản phẩm bên trái -----
            if (product.imageUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(product.imageUrl),
                    contentDescription = "Ảnh sản phẩm",
                    modifier = Modifier
                        .size(120.dp)
                        .background(BackgroundLight, shape = RoundedCornerShape(6.dp))
                )
            }

            Spacer(Modifier.width(12.dp))

            // ----- Thông tin sản phẩm bên phải -----
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Tên: ${product.name}", color = SecondaryDark, fontSize = 16.sp, maxLines = 1)
                Text("Giá: ${product.price}", color = SecondaryDark)
                Text("Loại: ${product.type}", color = SecondaryDark)
                Text("Danh mục: ${product.category}", color = SecondaryDark)
                Text("Tồn kho: ${product.stock}", color = SecondaryDark)
            }

            // ----- Nút sửa / xóa -----
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { onEditClicked(product) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = PrimaryMaroon)
                }
                IconButton(onClick = { onDeleteClicked(product) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = StatusError)
                }
            }
        }
    }
}
