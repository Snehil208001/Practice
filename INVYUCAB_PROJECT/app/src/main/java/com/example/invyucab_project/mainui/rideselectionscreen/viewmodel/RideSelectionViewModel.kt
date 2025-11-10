package com.example.invyucab_project.mainui.rideselectionscreen.viewmodel

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.invyucab_project.core.base.BaseViewModel
import com.example.invyucab_project.core.common.Resource
import com.example.invyucab_project.domain.model.RideOption
import com.example.invyucab_project.domain.model.RideSelectionState
import com.example.invyucab_project.domain.usecase.GetDirectionsAndRouteUseCase
import com.example.invyucab_project.domain.usecase.GetPlaceDetailsUseCase
import com.example.invyucab_project.domain.usecase.GetRidePricingUseCase
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class RideSelectionViewModel @Inject constructor(
    private val getPlaceDetailsUseCase: GetPlaceDetailsUseCase,
    private val getDirectionsAndRouteUseCase: GetDirectionsAndRouteUseCase,
    private val getRidePricingUseCase: GetRidePricingUseCase,
    private val fusedLocationClient: FusedLocationProviderClient,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(RideSelectionState())
    val uiState = _uiState.asStateFlow()

    private val dropPlaceId: String? = savedStateHandle.get<String>("dropPlaceId")
    private val encodedDropDescription: String? = savedStateHandle.get<String>("dropDescription")
    private val dropDescription: String = decodeUrlString(encodedDropDescription ?: "")

    private val pickupPlaceId: String? = savedStateHandle.get<String>("pickupPlaceId")
    private val encodedPickupDescription: String? = savedStateHandle.get<String>("pickupDescription")
    private val pickupDescription: String = decodeUrlString(encodedPickupDescription ?: "Your Current Location")

    private val isPickupCurrentLocation = pickupPlaceId == "current_location" || pickupPlaceId == null

    private val initialRideOptions = listOf(
        RideOption(1, Icons.Default.TwoWheeler, "Bike", "2 mins away", subtitle = "Quick Bike rides"),
        RideOption(2, Icons.Default.ElectricRickshaw, "Auto", "2 mins away", subtitle = "Affordable Auto rides"),
        RideOption(3, Icons.Default.LocalTaxi, "Cab Economy", "2 mins away", subtitle = "Comfy, economical"),
        RideOption(4, Icons.Default.Stars, "Cab Premium", "5 mins away", subtitle = "Spacious & top-rated")
    )

    private val _rideOptions = MutableStateFlow(initialRideOptions)
    val rideOptions = _rideOptions.asStateFlow()

    init {
        if (dropPlaceId == null || encodedDropDescription == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Drop location is missing. Please go back.",
                isLoading = false,
                isFetchingLocation = false
            )
        } else {
            _uiState.value = _uiState.value.copy(
                dropDescription = dropDescription,
                pickupDescription = pickupDescription
            )
            if (isPickupCurrentLocation) {
                getCurrentLocationAndProceed()
            } else {
                fetchSpecificLocationsAndRoute()
            }
        }
    }

    private fun decodeUrlString(encoded: String): String {
        return try {
            URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            encoded
        }
    }

    // --- Location Fetching (Stays in VM due to Context/Activity coupling) ---
    @SuppressLint("MissingPermission")
    private fun getCurrentLocationAndProceed() {
        _uiState.value = _uiState.value.copy(isFetchingLocation = true)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                _uiState.value = _uiState.value.copy(isFetchingLocation = false)
                onLocationsReady(currentLatLng, "place_id:$dropPlaceId")
            } else {
                requestNewLocation()
            }
        }.addOnFailureListener { requestNewLocation() }
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
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    _uiState.value = _uiState.value.copy(isFetchingLocation = false)
                    onLocationsReady(currentLatLng, "place_id:$dropPlaceId")
                } ?: run {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Could not fetch current location.",
                        isFetchingLocation = false
                    )
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    // --- API Calls (Now use UseCases) ---
    private fun fetchSpecificLocationsAndRoute() {
        if (pickupPlaceId == null || dropPlaceId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Missing Location", isLoading = false, isFetchingLocation = false)
            return
        }

        // Fetch both pickup and drop LatLng
        val pickupFlow = getPlaceDetailsUseCase.invoke(pickupPlaceId)
        val dropFlow = getPlaceDetailsUseCase.invoke(dropPlaceId)

        viewModelScope.launch {
            pickupFlow.zip(dropFlow) { pickupResult, dropResult ->
                Pair(pickupResult, dropResult)
            }.onEach { (pickupResult, dropResult) ->
                when {
                    pickupResult is Resource.Loading || dropResult is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, isFetchingLocation = false)
                    }
                    pickupResult is Resource.Success && dropResult is Resource.Success -> {
                        if (pickupResult.data != null && dropResult.data != null) {
                            _uiState.value = _uiState.value.copy(
                                pickupLocation = pickupResult.data,
                                dropLocation = dropResult.data
                            )
                            onLocationsReady(pickupResult.data, "place_id:$dropPlaceId")
                        } else {
                            _uiState.value = _uiState.value.copy(errorMessage = "Could not get location details.", isLoading = false)
                        }
                    }
                    pickupResult is Resource.Error -> _uiState.value = _uiState.value.copy(errorMessage = pickupResult.message, isLoading = false)
                    dropResult is Resource.Error -> _uiState.value = _uiState.value.copy(errorMessage = dropResult.message, isLoading = false)
                }
            }.launchIn(viewModelScope)
        }
    }

    private fun onLocationsReady(pickupLatLng: LatLng, destinationString: String) {
        if (dropPlaceId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Drop-off location ID is missing.", isLoading = false)
            return
        }

        // Get Route Info
        getDirectionsAndRouteUseCase.invoke(pickupLatLng, dropPlaceId).onEach { result ->
            when (result) {
                is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                is Resource.Success -> {
                    val routeInfo = result.data!!
                    _uiState.value = _uiState.value.copy(
                        routePolyline = routeInfo.polyline,
                        tripDurationSeconds = routeInfo.durationSeconds,
                        tripDistanceMeters = routeInfo.distanceMeters,
                        isLoading = false,
                        errorMessage = null
                    )
                    // Now fetch prices
                    fetchAllRideData(pickupLatLng, _uiState.value.dropLocation!!, routeInfo.durationSeconds, routeInfo.distanceMeters)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = result.message, isLoading = false)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun fetchAllRideData(pickup: LatLng, drop: LatLng, durationSeconds: Int?, distanceMeters: Int?) {
        val durationMinutes = durationSeconds?.let { (it / 60.0).roundToInt() }
        val distanceKm = (distanceMeters ?: 0) / 1000.0
        val distanceString = "%.1f km".format(distanceKm)

        // Set initial loading state for prices
        _rideOptions.update { currentOptions ->
            currentOptions.map {
                it.copy(
                    isLoadingPrice = true,
                    estimatedDurationMinutes = durationMinutes,
                    estimatedDistanceKm = distanceString
                )
            }
        }

        // Call UseCase
        getRidePricingUseCase.invoke(pickup, drop, _rideOptions.value).onEach { result ->
            when (result) {
                is Resource.Loading -> { /* Handled above */ }
                is Resource.Success -> {
                    _rideOptions.value = result.data!! // Success, update with prices
                }
                is Resource.Error -> {
                    // Error, update with N/A
                    _apiError.value = result.message
                    _rideOptions.update { currentOptions ->
                        currentOptions.map {
                            it.copy(
                                price = "N/A",
                                isLoadingPrice = false
                            )
                        }
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
        _apiError.value = null
    }
}