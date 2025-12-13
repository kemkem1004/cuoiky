package com.example.thigiuaki

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.filled.Description
import com.example.thigiuaki.ui.screens.ProfileScreen

sealed class CustomerScreen {
    object Products : CustomerScreen()
    object Cart : CustomerScreen()
    object Orders : CustomerScreen()
    object Profile : CustomerScreen()
}

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
    val userId = auth.currentUser?.uid ?: ""

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Sản phẩm") },
                    label = { Text("Sản phẩm") },
                    selected = currentScreen == CustomerScreen.Products,
                    onClick = { currentScreen = CustomerScreen.Products }
                )
                NavigationBarItem(
                    icon = { Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Giỏ hàng") },
                    label = { Text("Giỏ hàng") },
                    selected = currentScreen == CustomerScreen.Cart,
                    onClick = { currentScreen = CustomerScreen.Cart }
                )
                NavigationBarItem(
                    icon = { Icon(imageVector = Icons.Default.Description, contentDescription = "Đơn hàng") },
                    label = { Text("Đơn hàng") },
                    selected = currentScreen == CustomerScreen.Orders,
                    onClick = { currentScreen = CustomerScreen.Orders }
                )
                NavigationBarItem(
                    icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Tài khoản") },
                    label = { Text("Tài khoản") },
                    selected = currentScreen == CustomerScreen.Profile,
                    onClick = { currentScreen = CustomerScreen.Profile }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                is CustomerScreen.Products -> ProductListScreen(
                    onLogout = {},
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
            }
        }
    }
}

