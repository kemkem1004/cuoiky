package com.example.thigiuaki

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.ui.res.painterResource
import com.example.thigiuaki.ui.screens.ProductListScreen
import com.example.thigiuaki.ui.screens.ProfileScreen

// Tạm thời tạo các hằng số màu sắc ở đây để không gây lỗi biên dịch
private val BackgroundLight = Color(0xFFFAF9F6)
private val PrimaryMaroon = Color(0xFF8D021F)
private val IconSize = 26.dp
private val BarHeight = 72.dp
private val SecondaryDark = Color(0xFF424242)


// ********** LƯU Ý: ĐỂ KHẮC PHỤC LỖI XUNG ĐỘT (Conflicting Overloads) **********
// Các hàm Composable như ProfileScreen, ProductListScreen, v.v. đã được XÓA
// khỏi file này và giả định được import từ package com.example.thigiuaki.ui.screens
// ******************************************************************************


sealed class CustomerScreen {
    object Products : CustomerScreen()
    object Cart : CustomerScreen()
    object Orders : CustomerScreen()
    object Profile : CustomerScreen()
    object Messages : CustomerScreen()
}

// Hàm Top Bar tùy chỉnh (Đã thêm vào file này để tiện sử dụng)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerTopBar(
    onNavigateToWishlist: () -> Unit,
    onNavigateToCheckout: () -> Unit
) {
    TopAppBar(
        title = {
            // Khối 'title' chỉ nên chứa duy nhất Composable cần hiển thị
            androidx.compose.foundation.Image( // Sửa lỗi xung đột tên hàm Image
                // Thay thế R.drawable.logo1 bằng resource ID của ảnh logo của bạn
                painter = painterResource(id = R.drawable.logo1),
                contentDescription = "Logo Cửa Hàng",
                // Điều chỉnh kích thước theo ý muốn của bạn
                modifier = Modifier
                    .size(width = 180.dp, height = 32.dp) // Sửa lỗi: Thay thế autoSaver<>().dp bằng kích thước cố định (ví dụ: 32.dp)
                ,
                alignment = Alignment.CenterStart
            )
        }, // <-
        actions = {
            IconButton(onClick = onNavigateToWishlist) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Yêu thích", tint = SecondaryDark)
            }
            IconButton(onClick = onNavigateToCheckout) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Giỏ hàng", tint = PrimaryMaroon)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BackgroundLight
        )
    )
}

// ********** HÀM CHÍNH ĐÃ ĐƯỢC CHỈNH SỬA **********
@Composable
fun CustomerHomeScreen(
    onLogout: () -> Unit,
    onNavigateToProductDetails: (String) -> Unit,
    onNavigateToCheckout: () -> Unit = {},
    onNavigateToWishlist: () -> Unit = {},
    onNavigateToMessage: ((String, String, String) -> Unit)? = null
) {
    var currentScreen by remember { mutableStateOf<CustomerScreen>(CustomerScreen.Products) }
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: "TEMP_USER_ID" // Sử dụng ID an toàn

    Scaffold(
        topBar = {
            // Hiển thị TopBar tùy chỉnh chỉ trên màn hình Products
            if (currentScreen == CustomerScreen.Products) {
                CustomerTopBar(
                    onNavigateToWishlist = onNavigateToWishlist,
                    onNavigateToCheckout = onNavigateToCheckout
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = BackgroundLight, // Nền sáng
                modifier = Modifier.height(BarHeight)
            ) {
                val items = listOf(
                    Pair(CustomerScreen.Products, Icons.Default.Home to "Sản phẩm"),
                    Pair(CustomerScreen.Cart, Icons.Default.ShoppingCart to "Giỏ hàng"),
                    Pair(CustomerScreen.Orders, Icons.Default.Description to "Đơn hàng"),
                    Pair(CustomerScreen.Messages, Icons.Default.Message to "Tin nhắn"),
                    Pair(CustomerScreen.Profile, Icons.Default.Person to "Tài khoản"),
                )

                items.forEach { (screen, iconInfo) ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = iconInfo.first,
                                contentDescription = iconInfo.second,
                                modifier = Modifier.size(IconSize)
                            )
                        },
                        label = {
                            Text(
                                iconInfo.second,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            )
                        },
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = PrimaryMaroon,
                            indicatorColor = PrimaryMaroon.copy(alpha = 0.9f) // Màu nền chỉ báo Đỏ Gạch
                        )
                    )
                }
            }
        },
        containerColor = BackgroundLight // Nền chính
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (currentScreen) {
                is CustomerScreen.Products -> ProductListScreen(
                    onLogout = onLogout,
                    onNavigateToProductDetails = onNavigateToProductDetails
                )
                is CustomerScreen.Cart -> CartScreen(
                    onCheckout = onNavigateToCheckout,
                    onNavigateToProductDetails = onNavigateToProductDetails
                )
                is CustomerScreen.Orders -> OrderHistoryScreen()
                is CustomerScreen.Profile -> ProfileScreen(
                    onLogout = onLogout,
                    onNavigateToWishlist = onNavigateToWishlist
                )
                is CustomerScreen.Messages -> CustomerMessageScreen(userId = userId)
            }
        }
    }
}