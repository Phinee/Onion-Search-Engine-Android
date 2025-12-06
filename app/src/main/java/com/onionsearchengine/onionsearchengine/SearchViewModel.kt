package com.onionsearchengine.onionsearchengine


// In SearchViewModel.kt
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AppState {
    object Initializing : AppState()
    object Ready : AppState()
    data class Error(val message: String) : AppState()
}


sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<SearchResult>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

//class SearchViewModel : ViewModel() {
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val apiKeyManager = ApiKeyManager(application)
    private var apiKey: String? = apiKeyManager.getApiKey()

    private val _appState = MutableStateFlow<AppState>(AppState.Initializing)
    val appState: StateFlow<AppState> = _appState.asStateFlow()


    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentPage = 1
    private var totalPages = 1
    private var currentQuery = ""

    init {
        initializeApp()
    }

    private fun initializeApp() {
        if (apiKey != null) {
            _appState.value = AppState.Ready
        } else {
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.provisionDeviceKey()
                    if (response.status == "success" && response.api_key != null) {
                        apiKeyManager.saveApiKey(response.api_key)
                        apiKey = response.api_key
                        _appState.value = AppState.Ready
                    } else {
                        _appState.value = AppState.Error("Could not retrieve a valid API key.")
                    }
                } catch (e: Exception) {
                    _appState.value = AppState.Error("Failed to connect to server for initial setup.")
                }
            }
        }
    }



    fun newSearch(query: String) {

        if (apiKey == null) {
            _error.value = "API Key not available. Cannot perform search."
            return
        }

        currentQuery = query
        currentPage = 1
        totalPages = 1
        _results.value = emptyList()
        _error.value = null
        loadNextPage()
    }

    fun loadNextPage() {

        if (apiKey == null || _isLoading.value || currentPage > totalPages) {
            return
        }

        if (_isLoading.value || currentPage > totalPages) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitInstance.api.search(apiKey!!, currentQuery, currentPage)
                if (response.status == "success") {
                    _results.value = _results.value + response.results
                    totalPages = response.pagination.totalPages
                    currentPage++
                } else {
                    _error.value = "API returned an error."
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}