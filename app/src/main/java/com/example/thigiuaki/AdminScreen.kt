package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.thigiuaki.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

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
        "Ảnh mặc định 1" to "https://img.icons8.com/ios-filled/200/camera.png",
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
                title = { Text("Trang quản lý") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Đăng xuất")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Sản phẩm") },
                    label = { Text("Sản phẩm") },
                    selected = currentScreen == AdminScreenType.Products,
                    onClick = { currentScreen = AdminScreenType.Products }
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
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                is AdminScreenType.Products -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {

            // ----- Tiêu đề -----
            Text(
                if (editingProduct != null) "✏️ Chỉnh sửa sản phẩm:" else "➕ Thêm sản phẩm mới:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            // ----- Nhập dữ liệu -----
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Tên sản phẩm") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = newPrice,
                onValueChange = { newPrice = it },
                label = { Text("Giá") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Dropdown chọn ảnh mẫu
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

            Spacer(Modifier.height(8.dp))

            // Nhập URL ảnh Internet
            OutlinedTextField(
                value = newImageUrl,
                onValueChange = { newImageUrl = it },
                label = { Text("Hoặc nhập URL ảnh Internet (tùy chọn)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            val previewImageUrl = if (newImageUrl.isNotBlank()) newImageUrl else selectedImageUrl
            Image(
                painter = rememberAsyncImagePainter(previewImageUrl),
                contentDescription = "Ảnh xem trước",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = newType,
                onValueChange = { newType = it },
                label = { Text("Loại sản phẩm") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

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
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = newDescription,
                onValueChange = { newDescription = it },
                label = { Text("Mô tả") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = newSizes,
                onValueChange = { newSizes = it },
                label = { Text("Kích thước (phân cách bằng dấu phẩy)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("S,M,L,XL") }
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = newColors,
                onValueChange = { newColors = it },
                label = { Text("Màu sắc (phân cách bằng dấu phẩy)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Black,White,Blue") }
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = newStock,
                onValueChange = { newStock = it },
                label = { Text("Số lượng tồn kho") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // ----- Nút thêm / cập nhật -----
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
                        // 🔁 Cập nhật sản phẩm
                        db.collection("products")
                            .document(editingProduct!!.id)
                            .set(productToSave)
                            .addOnSuccessListener {
                                Log.d("Firestore", "✅ Cập nhật sản phẩm: ${editingProduct!!.id}")
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
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "❌ Lỗi cập nhật: ${e.message}")
                            }
                    } else {
                        // ➕ Thêm mới sản phẩm
                        db.collection("products")
                            .add(productToSave)
                            .addOnSuccessListener {
                                Log.d("Firestore", "✅ Đã thêm sản phẩm: ${it.id}")
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
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "❌ Lỗi thêm: ${e.message}")
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (editingProduct != null) "CẬP NHẬT SẢN PHẨM" else "THÊM SẢN PHẨM")
            }

            // Nếu đang sửa → nút Hủy
            if (editingProduct != null) {
                Spacer(Modifier.height(8.dp))
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
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("HỦY CHỈNH SỬA")
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider()
            Spacer(Modifier.height(10.dp))

            // ----- Danh sách sản phẩm -----
            Text("Danh sách sản phẩm:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                products.forEach { p ->
                    ProductAdminItem(
                        product = p,
                        onEditClicked = { prod ->
                            // 🔹 Khi bấm Sửa, đưa dữ liệu lên form
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
                        },
                        onDeleteClicked = { prod ->
                            if (prod.id.isNotBlank()) {
                                db.collection("products").document(prod.id).delete()
                            }
                        }
                    )
                    Divider()
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Tên sp: ${product.name}")
            Text("Giá: ${product.price}")
            Text("Loại: ${product.type}")
            if (product.imageUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(product.imageUrl),
                    contentDescription = "Ảnh sản phẩm",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }
        }
        IconButton(onClick = { onEditClicked(product) }) {
            Icon(Icons.Default.Edit, contentDescription = "Sửa")
        }
        IconButton(onClick = { onDeleteClicked(product) }) {
            Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.Red)
        }
    }
}
