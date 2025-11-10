package com.example.invyucab_project.domain.model

import com.google.android.gms.maps.model.LatLng

// ✅✅✅ START OF FIX ✅✅✅
// This data class was deleted but is still needed by the HomeScreen and HomeViewModel.
data class AutocompletePrediction(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val description: String // Full text
)
// ✅✅✅ END OF FIX ✅✅✅

data class HomeUiState(
    val currentLocation: LatLng? = null,
    val isFetchingLocation: Boolean = true,
    val isSearching: Boolean = false,
    val searchResults: List<AutocompletePrediction> = emptyList() // This will now resolve
)