package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.thigiuaki.model.Address
import com.example.thigiuaki.model.CartItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    onBack: () -> Unit,
    onOrderPlaced: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    var shippingAddress by remember { mutableStateOf(Address()) }
    var paymentMethod by remember { mutableStateOf("cash") }
    var addresses by remember { mutableStateOf(listOf<Address>()) }
    var isLoading by remember { mutableStateOf(false) }
    var showAddressDialog by remember { mutableStateOf(false) }
    var deliveryType by remember { mutableStateOf("home_delivery") }
    var pickupTime by remember { mutableStateOf("") }
    var orderNotes by remember { mutableStateOf("") }
    var showPickupTimeDialog by remember { mutableStateOf(false) }

    // Load user addresses
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            db.collection("addresses")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, _ ->
                    val addrList = snapshot?.documents?.mapNotNull { doc ->
                        val addr = doc.toObject<Address>()
                        addr?.copy(id = doc.id)
                    } ?: emptyList()
                    addresses = addrList
                    shippingAddress = addrList.firstOrNull { it.isDefault } ?: addrList.firstOrNull() ?: Address()
                }
        }
    }

    val totalAmount = cartItems.sumOf { it.price * it.quantity }

    val handlePlaceOrder: () -> Unit = handlePlaceOrder@{
        if (shippingAddress.fullName.isBlank() || shippingAddress.phone.isBlank() ||
            shippingAddress.street.isBlank() || shippingAddress.city.isBlank()) {
            // Hiển thị toast/Snackbar lỗi
            return@handlePlaceOrder
        }

        val now = Timestamp.now()
        var pickupTimestamp: Timestamp? = null

        if (deliveryType == "store_pickup" && pickupTime.isNotBlank()) {
            try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val pickupDate = dateFormat.parse(pickupTime)
                if (pickupDate != null) {
                    pickupTimestamp = Timestamp(pickupDate)
                }
            } catch (e: Exception) {
                Log.e("Checkout", "Error parsing pickup time: ${e.message}")
                return@handlePlaceOrder // Dừng nếu giờ lấy không hợp lệ
            }
        }

        isLoading = true
        val orderData = hashMapOf(
            "userId" to userId,
            // Sử dụng Map cho items, dựa vào CartItem.kt đã sửa constructor rỗng để ánh xạ đọc
            "items" to cartItems.map { item ->
                hashMapOf(
                    "productId" to item.productId,
                    "productName" to item.productName,
                    "productImageUrl" to item.productImageUrl,
                    "price" to item.price,
                    "quantity" to item.quantity,
                    "selectedSize" to item.selectedSize,
                    "selectedColor" to item.selectedColor
                )
            },
            "subtotal" to totalAmount,
            "totalAmount" to totalAmount,
            "shippingAddress" to hashMapOf(
                "fullName" to shippingAddress.fullName,
                "phone" to shippingAddress.phone,
                "street" to shippingAddress.street,
                "city" to shippingAddress.city,
                "district" to shippingAddress.district,
                "ward" to shippingAddress.ward
            ),
            "deliveryType" to deliveryType,
            "orderNotes" to orderNotes,
            "status" to "pending",
            "isProcessed" to false,
            "paymentMethod" to paymentMethod,
            "paymentStatus" to "pending",

            // FIX LỖI GHI DỮ LIỆU: Ghi orderDate là Timestamp
            "createdAt" to now,
            "orderDate" to now
        )

        if (pickupTimestamp != null) {
            orderData["pickupTime"] = pickupTimestamp
        }


        db.collection("orders").add(orderData)
            .addOnSuccessListener { orderDoc ->
                // Clear cart và Update product stock
                cartItems.forEach { item ->
                    db.collection("cart").document(item.id).delete()
                    db.collection("products").document(item.productId)
                        .get()
                        .addOnSuccessListener { productDoc ->
                            val currentStock = productDoc.getLong("stock")?.toInt() ?: 0
                            db.collection("products").document(item.productId)
                                .update("stock", currentStock - item.quantity)
                        }
                }

                Log.d("Checkout", "Order placed: ${orderDoc.id}")
                isLoading = false
                onOrderPlaced()
            }
            .addOnFailureListener { e ->
                Log.e("Checkout", "Error placing order: ${e.message}")
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thanh toán") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tổng tiền:", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${totalAmount.toInt()} VND",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = handlePlaceOrder,
                        enabled = !isLoading && shippingAddress.fullName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Đặt hàng")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Order Summary
            item {
                Text(
                    "Đơn hàng (${cartItems.size} sản phẩm)",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(cartItems) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${item.productName} x${item.quantity}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${(item.price * item.quantity).toInt()} VND",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item { Divider() }

            // Shipping Address
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Địa chỉ giao hàng",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = { showAddressDialog = true }) {
                        Text(if (addresses.isEmpty()) "Thêm địa chỉ" else "Chọn địa chỉ")
                    }
                }
            }

            item {
                if (shippingAddress.fullName.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                shippingAddress.fullName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(shippingAddress.phone)
                            Text("${shippingAddress.street}, ${shippingAddress.ward}, ${shippingAddress.district}, ${shippingAddress.city}")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showAddressDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Thêm địa chỉ giao hàng")
                    }
                }
            }

            item { Divider() }

            // Delivery Type
            item {
                Text(
                    "Hình thức nhận hàng",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = deliveryType == "home_delivery",
                        onClick = { deliveryType = "home_delivery" },
                        label = { Text("Giao hàng tận nơi") }
                    )
                    FilterChip(
                        selected = deliveryType == "store_pickup",
                        onClick = {
                            deliveryType = "store_pickup"
                            showPickupTimeDialog = true
                        },
                        label = { Text("Đến lấy") }
                    )
                }
            }

            if (deliveryType == "store_pickup") {
                item {
                    OutlinedTextField(
                        value = pickupTime,
                        onValueChange = { pickupTime = it },
                        label = { Text("Giờ đến lấy (dd/MM/yyyy HH:mm)") },
                        placeholder = { Text("Ví dụ: 25/12/2024 14:30") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            TextButton(onClick = { showPickupTimeDialog = true }) {
                                Text("Chọn giờ")
                            }
                        }
                    )
                }
            }

            item { Divider() }

            // Order Notes
            item {
                Text(
                    "Ghi chú đơn hàng",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            item {
                OutlinedTextField(
                    value = orderNotes,
                    onValueChange = { orderNotes = it },
                    label = { Text("Nhập ghi chú (tùy chọn)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }

            item { Divider() }

            // Payment Method
            item {
                Text(
                    "Phương thức thanh toán",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = paymentMethod == "cash",
                        onClick = { paymentMethod = "cash" },
                        label = { Text("Tiền mặt") }
                    )
                    FilterChip(
                        selected = paymentMethod == "card",
                        onClick = { paymentMethod = "card" },
                        label = { Text("Thẻ") }
                    )
                    FilterChip(
                        selected = paymentMethod == "online",
                        onClick = { paymentMethod = "online" },
                        label = { Text("Online") }
                    )
                }
            }
        }

        if (showAddressDialog) {
            AddressDialog(
                address = shippingAddress,
                onDismiss = { showAddressDialog = false },
                onSave = { address ->
                    val addressData = hashMapOf(
                        "userId" to userId,
                        "fullName" to address.fullName,
                        "phone" to address.phone,
                        "street" to address.street,
                        "city" to address.city,
                        "district" to address.district,
                        "ward" to address.ward,
                        "isDefault" to address.isDefault
                    )

                    if (address.id.isNotBlank()) {
                        db.collection("addresses").document(address.id).set(addressData)
                    } else {
                        db.collection("addresses").add(addressData)
                            .addOnSuccessListener { doc ->
                                shippingAddress = address.copy(id = doc.id)
                            }
                    }
                    showAddressDialog = false
                }
            )
        }

        if (showPickupTimeDialog) {
            PickupTimeDialog(
                onDismiss = { showPickupTimeDialog = false },
                onConfirm = { dateTime ->
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    pickupTime = dateFormat.format(dateTime)
                    showPickupTimeDialog = false
                }
            )
        }
    }
}

@Composable
fun PickupTimeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Date) -> Unit
) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedHour by remember { mutableStateOf(14) }
    var selectedMinute by remember { mutableStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn giờ đến lấy") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date picker would go here - simplified version
                Text("Ngày: ${selectedDate.get(Calendar.DAY_OF_MONTH)}/${selectedDate.get(Calendar.MONTH) + 1}/${selectedDate.get(Calendar.YEAR)}")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Giờ:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (selectedHour > 0) selectedHour-- }) {
                                Text("-")
                            }
                            Text("$selectedHour")
                            IconButton(onClick = { if (selectedHour < 23) selectedHour++ }) {
                                Text("+")
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Phút:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (selectedMinute > 0) selectedMinute -= 15 }) {
                                Text("-")
                            }
                            Text("$selectedMinute")
                            IconButton(onClick = { if (selectedMinute < 45) selectedMinute += 15 }) {
                                Text("+")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedDate.set(Calendar.HOUR_OF_DAY, selectedHour)
                    selectedDate.set(Calendar.MINUTE, selectedMinute)
                    onConfirm(selectedDate.time)
                }
            ) {
                Text("Xác nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun AddressDialog(
    address: Address,
    onDismiss: () -> Unit,
    onSave: (Address) -> Unit
) {
    var fullName by remember { mutableStateOf(address.fullName) }
    var phone by remember { mutableStateOf(address.phone) }
    var street by remember { mutableStateOf(address.street) }
    var city by remember { mutableStateOf(address.city) }
    var district by remember { mutableStateOf(address.district) }
    var ward by remember { mutableStateOf(address.ward) }
    var isDefault by remember { mutableStateOf(address.isDefault) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Địa chỉ giao hàng") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ và tên") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("Số nhà, đường") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ward,
                    onValueChange = { ward = it },
                    label = { Text("Phường/Xã") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text("Quận/Huyện") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Thành phố/Tỉnh") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                    Text("Đặt làm địa chỉ mặc định")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        address.copy(
                            fullName = fullName,
                            phone = phone,
                            street = street,
                            city = city,
                            district = district,
                            ward = ward,
                            isDefault = isDefault
                        )
                    )
                }
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}