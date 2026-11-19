package com.example.reportit.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.reportit.data.model.Complaint
import com.example.reportit.data.model.ComplaintStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class ComplaintRepository {

    private val client = SupabaseClientProvider.supabase
    private val storageBucket = "complaint-images"

    suspend fun submitComplaint(
        userId: String,
        title: String,
        description: String,
        category: String,
        imageUri: Uri?,
        latitude: Double?,
        longitude: Double?,
        context: Context
    ): Result<Complaint> = runCatching {
        val imageUrl = imageUri?.let { uploadImage(it, context) }

        val complaint = Complaint(
            user_id = userId,
            title = title,
            description = description,
            category = category,
            status = ComplaintStatus.PENDING.name,
            image_url = imageUrl,
            location_latitude = latitude,
            location_longitude = longitude
        )

        client.from("complaints").insert(complaint) {
            select()
        }.decodeSingle<Complaint>()
    }

    suspend fun updateComplaint(
        complaintId: String,
        title: String,
        description: String,
        category: String,
        imageUri: Uri?,
        latitude: Double?,
        longitude: Double?,
        context: Context
    ): Result<Complaint> = runCatching {
        val imageUrl = imageUri?.let { uploadImage(it, context) }

        val updates = buildJsonObject {
            put("title", title)
            put("description", description)
            put("category", category)
            if (latitude != null) put("location_latitude", latitude)
            if (longitude != null) put("location_longitude", longitude)
            if (imageUrl != null) put("image_url", imageUrl)
        }

        client.from("complaints").update(updates) {
            select()
            filter { eq("id", complaintId) }
        }.decodeSingle<Complaint>()
    }

    suspend fun deleteComplaint(complaintId: String): Result<Unit> = runCatching {
        client.from("complaints").delete {
            filter { eq("id", complaintId) }
        }
    }

    suspend fun getComplaints(userId: String): Result<List<Complaint>> = runCatching {
        client.from("complaints").select {
            filter { eq("user_id", userId) }
            order("created_at", Order.DESCENDING)
        }.decodeList<Complaint>()
    }

    suspend fun getComplaintById(complaintId: String): Result<Complaint> = runCatching {
        client.from("complaints").select {
            filter { eq("id", complaintId) }
        }.decodeSingle<Complaint>()
    }

    fun observeComplaints(userId: String): Flow<List<Complaint>> = channelFlow {
        // Send initial data
        val initial = getComplaints(userId).getOrDefault(emptyList())
        send(initial)

        // Listen for realtime changes
        launch {
            try {
                val channel = client.channel("complaints_changes")

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "complaints"
                }

                channel.subscribe()

                changeFlow.collect { _ ->
                    // Re-fetch on any change
                    val complaints = getComplaints(userId).getOrDefault(emptyList())
                    send(complaints)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Realtime error: ${e.message}", e)
                // Already sent initial data above, so just log the error
            }
        }
    }

    private suspend fun uploadImage(imageUri: Uri, context: Context): String? = runCatching {
        val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
        val bytes = inputStream.readBytes()
        inputStream.close()

        val fileName = "${System.currentTimeMillis()}_${(0..999999).random()}.jpg"

        client.storage.from(storageBucket).upload(fileName, bytes) {
            upsert = true
        }

        client.storage.from(storageBucket).publicUrl(fileName)
    }.getOrNull()

    companion object {
        private const val TAG = "ComplaintRepository"
    }
}