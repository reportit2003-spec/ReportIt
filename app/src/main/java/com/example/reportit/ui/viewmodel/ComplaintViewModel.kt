package com.example.reportit.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reportit.data.local.LocalCacheManager
import com.example.reportit.data.model.Complaint
import com.example.reportit.data.model.ComplaintStatus
import com.example.reportit.data.model.DraftComplaint
import com.example.reportit.data.repository.AuthRepository
import com.example.reportit.data.repository.ComplaintRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ComplaintViewModel : ViewModel() {

    private val complaintRepository = ComplaintRepository()
    private val authRepository = AuthRepository()

    private val _complaints = MutableStateFlow<List<Complaint>>(emptyList())
    val complaints: StateFlow<List<Complaint>> = _complaints.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<ComplaintStatus?>(null)
    val selectedStatusFilter: StateFlow<ComplaintStatus?> = _selectedStatusFilter.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _operationSuccess = MutableStateFlow(false)
    val operationSuccess: StateFlow<Boolean> = _operationSuccess.asStateFlow()

    init {
        loadComplaints()
    }

    fun loadComplaints() {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            complaintRepository.getComplaints(userId)
                .onSuccess { complaints ->
                    _complaints.value = complaints
                    LocalCacheManager.cacheComplaints(complaints)
                }
                .onFailure { error ->
                    val cached = LocalCacheManager.getCachedComplaints()
                    if (cached.isNotEmpty()) {
                        _complaints.value = cached
                    }
                    _errorMessage.value = error.message
                }
            _isLoading.value = false
        }
    }

    fun observeComplaints() {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            complaintRepository.observeComplaints(userId).collect { updated ->
                _complaints.value = updated
                LocalCacheManager.cacheComplaints(updated)
            }
        }
    }

    fun setStatusFilter(status: ComplaintStatus?) {
        _selectedStatusFilter.value = status
    }

    fun getFilteredComplaints(): List<Complaint> {
        val filter = _selectedStatusFilter.value
        return if (filter == null) {
            _complaints.value
        } else {
            _complaints.value.filter { it.complaintStatus == filter }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _operationSuccess.value = false
    }
}

class SubmitComplaintViewModel : ViewModel() {

    private val complaintRepository = ComplaintRepository()
    private val authRepository = AuthRepository()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _category = MutableStateFlow("Other")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri: StateFlow<Uri?> = _imageUri.asStateFlow()

    private val _latitude = MutableStateFlow<Double?>(null)
    val latitude: StateFlow<Double?> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow<Double?>(null)
    val longitude: StateFlow<Double?> = _longitude.asStateFlow()

    private val _locationText = MutableStateFlow<String?>(null)
    val locationText: StateFlow<String?> = _locationText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _operationSuccess = MutableStateFlow(false)
    val operationSuccess: StateFlow<Boolean> = _operationSuccess.asStateFlow()

    private val _existingComplaint = MutableStateFlow<Complaint?>(null)
    val existingComplaint: StateFlow<Complaint?> = _existingComplaint.asStateFlow()

    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation.asStateFlow()

    init {
        loadDraft()
    }

    // ✅ NEW FUNCTION ADDED - Load complaint by ID for editing
    /**
     * Load complaint by ID for editing mode.
     * Handles repository call, loading states, and error management internally.
     * The screen simply calls this method without worrying about data layer details.
     */
    fun loadComplaintById(complaintId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            complaintRepository.getComplaintById(complaintId)
                .onSuccess { complaint ->
                    loadComplaintForEdit(complaint)
                }
                .onFailure {
                    _errorMessage.value = "Failed to load complaint"
                }
            _isLoading.value = false
        }
    }

    fun loadComplaintForEdit(complaint: Complaint) {
        _existingComplaint.value = complaint
        _title.value = complaint.title
        _description.value = complaint.description
        _category.value = complaint.category
        _latitude.value = complaint.location_latitude
        _longitude.value = complaint.location_longitude
    }

    fun updateTitle(value: String) {
        _title.value = value
        autoSaveDraft()
    }

    fun updateDescription(value: String) {
        _description.value = value
        autoSaveDraft()
    }

    fun updateCategory(value: String) {
        _category.value = value
        autoSaveDraft()
    }

    fun setImageUri(uri: Uri?) {
        _imageUri.value = uri
        autoSaveDraft()
    }

    fun setLocation(lat: Double?, lng: Double?, text: String?) {
        _latitude.value = lat
        _longitude.value = lng
        _locationText.value = text
        autoSaveDraft()
    }

    private var draftSaveJob: Job? = null
    private fun autoSaveDraft() {
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(1000)
            saveDraft()
        }
    }

    private fun loadDraft() {
        val draft = LocalCacheManager.getDraft()
        if (draft != null) {
            _title.value = draft.title
            _description.value = draft.description
            _category.value = draft.category
            _latitude.value = draft.latitude
            _longitude.value = draft.longitude
        }
    }

    private fun saveDraft() {
        LocalCacheManager.saveDraft(
            DraftComplaint(
                title = _title.value,
                description = _description.value,
                category = _category.value,
                imageUri = _imageUri.value?.toString(),
                latitude = _latitude.value,
                longitude = _longitude.value
            )
        )
    }

    fun validateInputs(): Boolean {
        return when {
            _title.value.isBlank() -> {
                _errorMessage.value = "Title is required"
                false
            }
            _description.value.isBlank() -> {
                _errorMessage.value = "Description is required"
                false
            }
            _category.value.isBlank() -> {
                _errorMessage.value = "Please select a category"
                false
            }
            else -> true
        }
    }

    fun submitComplaint(context: Context) {
        if (!validateInputs()) return
        saveDraft()

        val userId = authRepository.currentUserId ?: run {
            _errorMessage.value = "Not authenticated"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            complaintRepository.submitComplaint(
                userId = userId,
                title = _title.value.trim(),
                description = _description.value.trim(),
                category = _category.value,
                imageUri = _imageUri.value,
                latitude = _latitude.value,
                longitude = _longitude.value,
                context = context
            )
                .onSuccess {
                    LocalCacheManager.clearDraft()
                    _operationSuccess.value = true
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to submit complaint"
                }
            _isLoading.value = false
        }
    }

    fun updateComplaint(context: Context) {
        val complaint = _existingComplaint.value ?: return
        if (!validateInputs()) return

        viewModelScope.launch {
            _isLoading.value = true
            complaintRepository.updateComplaint(
                complaintId = complaint.id,
                title = _title.value.trim(),
                description = _description.value.trim(),
                category = _category.value,
                imageUri = _imageUri.value,
                latitude = _latitude.value,
                longitude = _longitude.value,
                context = context
            )
                .onSuccess {
                    _operationSuccess.value = true
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to update complaint"
                }
            _isLoading.value = false
        }
    }

    fun deleteComplaint() {
        val complaint = _existingComplaint.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            complaintRepository.deleteComplaint(complaint.id)
                .onSuccess {
                    _operationSuccess.value = true
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to delete complaint"
                }
            _isLoading.value = false
        }
    }

    fun showDeleteConfirm() { _showDeleteConfirmation.value = true }
    fun hideDeleteConfirm() { _showDeleteConfirmation.value = false }

    fun clearError() { _errorMessage.value = null }
    fun clearSuccess() { _operationSuccess.value = false }
}