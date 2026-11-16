package com.example.reportit.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Complaint(
    val id: String = "",
    val user_id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = ComplaintCategory.OTHER.name,
    val status: String = ComplaintStatus.PENDING.name,
    val image_url: String? = null,
    val location_latitude: Double? = null,
    val location_longitude: Double? = null,
    val created_at: String = "",
    val updated_at: String = ""
) {
    val complaintStatus: ComplaintStatus get() = ComplaintStatus.fromString(status)
    val complaintCategory: ComplaintCategory get() = ComplaintCategory.fromString(category)
}