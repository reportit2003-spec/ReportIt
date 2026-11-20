package com.example.reportit.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.reportit.data.model.Complaint
import com.example.reportit.data.model.DraftComplaint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object LocalCacheManager {

    private const val PREFS_NAME = "reportit_cache"
    private const val KEY_DRAFT = "draft_complaint"
    private const val KEY_RECENT_COMPLAINTS = "recent_complaints"

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveDraft(draft: DraftComplaint) {
        prefs.edit().putString(KEY_DRAFT, json.encodeToString(draft)).apply()
    }

    fun getDraft(): DraftComplaint? {
        val draftJson = prefs.getString(KEY_DRAFT, null) ?: return null
        return try {
            json.decodeFromString<DraftComplaint>(draftJson)
        } catch (e: Exception) {
            null
        }
    }

    fun clearDraft() {
        prefs.edit().remove(KEY_DRAFT).apply()
    }

    fun cacheComplaints(complaints: List<Complaint>) {
        try {
            val jsonStr = json.encodeToString(complaints)
            prefs.edit().putString(KEY_RECENT_COMPLAINTS, jsonStr).apply()
        } catch (e: Exception) {
        }
    }

    fun getCachedComplaints(): List<Complaint> {
        val jsonStr = prefs.getString(KEY_RECENT_COMPLAINTS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<Complaint>>(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearAllCache() {
        prefs.edit().clear().apply()
    }
}