package com.example.invyucab_project.mainui.homescreen.viewmodel

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.invyucab_project.core.base.BaseViewModel
import com.example.invyucab_project.core.common.Resource
import com.example.invyucab_project.core.navigations.Screen
import com.example.invyucab_project.data.models.Prediction // ✅ ADDED Import
import com.example.invyucab_project.domain.model.AutocompletePrediction // ✅ ADDED Import
import com.example.invyucab_project.domain.model.HomeUiState
import com.example.invyucab_project.domain.usecase.GetAutocompletePredictionsUseCase
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val getAutocompletePredictionsUseCase: GetAutocompletePredictionsUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    var searchQuery by mutableStateOf("")
        private set

    init {
        getCurrentLocation()
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        _uiState.update { it.copy(isFetchingLocation = true) }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                _uiState.update {
                    it.copy(
                        currentLocation = LatLng(location.latitude, location.longitude),
                        isFetchingLocation = false
                    )
                }
            } else {
                requestNewLocation()
            }
        }.addOnFailureListener {
            _apiError.value = "Failed to get location. Please enable GPS."
            _uiState.update { it.copy(isFetchingLocation = false) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocation() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdates(1)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                locationResult.lastLocation?.let { location ->
                    _uiState.update {
                        it.copy(
                            currentLocation = LatLng(location.latitude, location.longitude),
                            isFetchingLocation = false
                        )
                    }
                } ?: run {
                    _apiError.value = "Could not fetch current location."
                    _uiState.update { it.copy(isFetchingLocation = false) }
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun onQueryChange(query: String) {
        searchQuery = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300L)
            if (query.length > 2) {

                // ✅✅✅ FIX 1: Convert LatLng to String ✅✅✅
                val location = uiState.value.currentLocation
                val locationString = location?.let { "${it.latitude},${it.longitude}" } ?: ""

                getAutocompletePredictionsUseCase(query, locationString)
                    .onEach { result ->
                        when (result) {
                            is Resource.Loading -> {
                                _uiState.update { it.copy(isSearching = true) }
                            }
                            is Resource.Success -> {
                                // ✅✅✅ FIX 2: Map data.models.Prediction to domain.model.AutocompletePrediction ✅✅✅
                                val mappedPredictions = result.data?.map { prediction ->
                                    AutocompletePrediction(
                                        placeId = prediction.placeId,
                                        primaryText = prediction.structuredFormatting.mainText,
                                        secondaryText = prediction.structuredFormatting.secondaryText,
                                        description = prediction.description
                                    )
                                } ?: emptyList()

                                _uiState.update {
                                    it.copy(
                                        searchResults = mappedPredictions, // Use the mapped list
                                        isSearching = false
                                    )
                                }
                            }
                            is Resource.Error -> {
                                _apiError.value = result.message
                                _uiState.update { it.copy(isSearching = false) }
                            }
                        }
                    }.launchIn(viewModelScope)
            } else {
                _uiState.update { it.copy(searchResults = emptyList()) }
            }
        }
    }

    fun onClearQuery() {
        searchQuery = ""
        _uiState.update { it.copy(searchResults = emptyList()) }
    }

    fun onPredictionTapped(placeId: String, description: String) {
        viewModelScope.launch {
            sendEvent(
                UiEvent.Navigate(
                    Screen.RideSelectionScreen.createRoute(
                        dropPlaceId = placeId,
                        dropDescription = description,
                        pickupPlaceId = "current_location",
                        pickupDescription = "Your Current Location"
                    )
                ))
        }
    }
}