package com.example.thigiuaki // Đảm bảo package này đúng

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Định dạng Timestamp của Firebase thành chuỗi ngày giờ dễ đọc.
 */
fun Timestamp?.toDateTimeString(format: String = "dd/MM/yyyy HH:mm"): String {
    return if (this != null) {
        try {
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            return sdf.format(this.toDate())
        } catch (e: Exception) {
            "Lỗi định dạng"
        }
    } else {
        "N/A"
    }
}