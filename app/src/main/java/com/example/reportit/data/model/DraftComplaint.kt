package com.example.reportit.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DraftComplaint(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val imageUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)