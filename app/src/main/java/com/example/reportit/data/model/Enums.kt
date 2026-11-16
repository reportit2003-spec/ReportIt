package com.example.reportit.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ComplaintStatus(val displayName: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved");

    companion object {
        fun fromString(value: String): ComplaintStatus =
            entries.firstOrNull { it.name == value } ?: PENDING
    }
}

@Serializable
enum class ComplaintCategory(val displayName: String) {
    INFRASTRUCTURE("Infrastructure"),
    HOSTEL("Hostel"),
    CLEANLINESS("Cleanliness"),
    SECURITY("Security"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): ComplaintCategory =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}