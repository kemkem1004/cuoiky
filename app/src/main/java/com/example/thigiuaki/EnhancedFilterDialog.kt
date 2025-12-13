package com.example.thigiuaki

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.thigiuaki.model.Product

data class FilterOptions(
    var category: String? = null,
    var subCategory: String? = null,
    var brand: String? = null,
    var material: String? = null,
    var minPrice: Double? = null,
    var maxPrice: Double? = null,
    var sizes: List<String> = emptyList(),
    var colors: List<String> = emptyList(),
    var sortBy: String = "name" // name, price_asc, price_desc, newest, rating
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedFilterDialog(
    products: List<Product>,
    currentFilters: FilterOptions,
    onDismiss: () -> Unit,
    onApplyFilters: (FilterOptions) -> Unit
) {
    var filters by remember { mutableStateOf(currentFilters) }
    
    val categories = remember(products) {
        products.map { it.category }.distinct().filter { it.isNotBlank() }
    }
    
    val brands = remember(products) {
        products.map { it.brand }.distinct().filter { it.isNotBlank() }
    }
    
    val materials = remember(products) {
        products.map { it.material }.distinct().filter { it.isNotBlank() }
    }
    
    val allSizes = remember(products) {
        products.flatMap { it.sizes }.distinct().filter { it.isNotBlank() }
    }
    
    val allColors = remember(products) {
        products.flatMap { it.colors }.distinct().filter { it.isNotBlank() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lọc sản phẩm") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category Filter
                if (categories.isNotEmpty()) {
                    Text("Danh mục:", style = MaterialTheme.typography.titleSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = filters.category == null,
                            onClick = { filters = filters.copy(category = null) },
                            label = { Text("Tất cả") }
                        )
                        categories.forEach { category ->
                            FilterChip(
                                selected = filters.category == category,
                                onClick = { 
                                    filters = filters.copy(
                                        category = if (filters.category == category) null else category
                                    )
                                },
                                label = { Text(category) }
                            )
                        }
                    }
                }

                // Brand Filter
                if (brands.isNotEmpty()) {
                    Text("Thương hiệu:", style = MaterialTheme.typography.titleSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = filters.brand == null,
                            onClick = { filters = filters.copy(brand = null) },
                            label = { Text("Tất cả") }
                        )
                        brands.forEach { brand ->
                            FilterChip(
                                selected = filters.brand == brand,
                                onClick = { 
                                    filters = filters.copy(
                                        brand = if (filters.brand == brand) null else brand
                                    )
                                },
                                label = { Text(brand) }
                            )
                        }
                    }
                }

                // Material Filter
                if (materials.isNotEmpty()) {
                    Text("Chất liệu:", style = MaterialTheme.typography.titleSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = filters.material == null,
                            onClick = { filters = filters.copy(material = null) },
                            label = { Text("Tất cả") }
                        )
                        materials.forEach { material ->
                            FilterChip(
                                selected = filters.material == material,
                                onClick = { 
                                    filters = filters.copy(
                                        material = if (filters.material == material) null else material
                                    )
                                },
                                label = { Text(material) }
                            )
                        }
                    }
                }

                // Price Range
                Text("Khoảng giá:", style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = filters.minPrice?.toString() ?: "",
                        onValueChange = { 
                            filters = filters.copy(minPrice = it.toDoubleOrNull())
                        },
                        label = { Text("Từ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text(" - ")
                    OutlinedTextField(
                        value = filters.maxPrice?.toString() ?: "",
                        onValueChange = { 
                            filters = filters.copy(maxPrice = it.toDoubleOrNull())
                        },
                        label = { Text("Đến") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Size Filter
                if (allSizes.isNotEmpty()) {
                    Text("Kích thước:", style = MaterialTheme.typography.titleSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        allSizes.forEach { size ->
                            FilterChip(
                                selected = filters.sizes.contains(size),
                                onClick = { 
                                    filters = filters.copy(
                                        sizes = if (filters.sizes.contains(size)) {
                                            filters.sizes - size
                                        } else {
                                            filters.sizes + size
                                        }
                                    )
                                },
                                label = { Text(size) }
                            )
                        }
                    }
                }

                // Color Filter
                if (allColors.isNotEmpty()) {
                    Text("Màu sắc:", style = MaterialTheme.typography.titleSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        allColors.forEach { color ->
                            FilterChip(
                                selected = filters.colors.contains(color),
                                onClick = { 
                                    filters = filters.copy(
                                        colors = if (filters.colors.contains(color)) {
                                            filters.colors - color
                                        } else {
                                            filters.colors + color
                                        }
                                    )
                                },
                                label = { Text(color) }
                            )
                        }
                    }
                }

                // Sort Options
                Text("Sắp xếp:", style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "name" to "Tên",
                        "price_asc" to "Giá tăng",
                        "price_desc" to "Giá giảm",
                        "newest" to "Mới nhất",
                        "rating" to "Đánh giá cao"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = filters.sortBy == value,
                            onClick = { filters = filters.copy(sortBy = value) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onApplyFilters(filters) }) {
                Text("Áp dụng")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

