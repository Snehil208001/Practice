package com.example.invyucab_project.mainui.homescreen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.invyucab_project.core.base.BaseViewModel
import com.example.invyucab_project.core.utils.navigationsbar.AppBottomNavigation
import com.example.invyucab_project.domain.model.AutocompletePrediction
import com.example.invyucab_project.domain.model.SearchField
import com.example.invyucab_project.mainui.homescreen.viewmodel.HomeViewModel
import com.example.invyucab_project.ui.theme.CabMintGreen
import com.example.invyucab_project.ui.theme.CabVeryLightMint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Collect navigation events
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is BaseViewModel.UiEvent.Navigate -> {
                    navController.navigate(event.route)
                }
                else -> {}
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            // ✅ ADDED the Bottom Navigation Bar
            AppBottomNavigation(navController = navController, selectedItem = "Home")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding) // Apply padding from the main Scaffold
                // ✅✅✅ START OF FIX (for cutout) ✅✅✅
                // Use 'or' correctly with the imported WindowInsetsSides
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    )
                )
            // ✅✅✅ END OF FIX ✅✅✅
        ) {
            SearchHeader(
                pickupQuery = uiState.pickupQuery,
                dropQuery = uiState.dropQuery,
                onPickupQueryChange = viewModel::onPickupQueryChange,
                onDropQueryChange = viewModel::onDropQueryChange,
                onClearPickup = viewModel::onClearPickup,
                onClearDrop = viewModel::onClearDrop,
                onFocusChange = viewModel::onFocusChange,
                isSearching = uiState.isSearching,
                onBack = {
                    // This button doesn't do anything on Home, as per the design
                }
            )

            // Show results based on which field is active
            val results = if (uiState.activeField == SearchField.PICKUP) {
                uiState.pickupResults
            } else {
                uiState.dropResults
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    // ✅ FIX for Keyboard: Moves list up when keyboard is open
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(results) { prediction ->
                    PredictionItem(
                        prediction = prediction,
                        onClick = {
                            viewModel.onPredictionTapped(prediction)
                        }
                    )
                }

                item {
                    AddressRow(
                        icon = Icons.Outlined.Place,
                        title = "Set on map",
                        subtitle = "Choose your location on map",
                        onClick = { /* TODO: Handle map selection */ }
                    )
                }
                item {
                    AddressRow(
                        icon = Icons.Default.Star,
                        title = "Saved Places",
                        subtitle = "Manage your saved locations",
                        onClick = { /* TODO: Handle saved places */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(
    pickupQuery: String,
    dropQuery: String,
    onPickupQueryChange: (String) -> Unit,
    onDropQueryChange: (String) -> Unit,
    onClearPickup: () -> Unit,
    onClearDrop: () -> Unit,
    onFocusChange: (SearchField) -> Unit,
    isSearching: Boolean,
    onBack: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CabVeryLightMint)
            .padding(16.dp)
    ) {
        // This Row contains the icon and the text fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon on the left (as seen in your screenshot)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.padding(end = 16.dp).clickable { onBack() }
            )

            Column(modifier = Modifier.weight(1f)) {

                // --- Pickup Location Field ---
                SearchTextField(
                    query = pickupQuery,
                    onQueryChange = onPickupQueryChange,
                    onClear = onClearPickup,
                    onFocus = { onFocusChange(SearchField.PICKUP) },
                    placeholder = "Enter pickup location",
                    isSearching = isSearching,
                    keyboardController = keyboardController
                )

                Divider(color = Color.LightGray)

                // --- Drop Location Field ---
                SearchTextField(
                    query = dropQuery,
                    onQueryChange = onDropQueryChange,
                    onClear = onClearDrop,
                    onFocus = { onFocusChange(SearchField.DROP) },
                    placeholder = "Enter drop location",
                    isSearching = isSearching,
                    keyboardController = keyboardController,
                    autoFocus = true // Auto-focus this field
                )
            }
        }
    }
}

@Composable
private fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onFocus: () -> Unit,
    placeholder: String,
    isSearching: Boolean,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    autoFocus: Boolean = false // Added parameter for auto-focus
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocus() },
        placeholder = { Text(placeholder) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = CabMintGreen
                    )
                }
                // Show 'X' if query is not empty AND not the default "Your Current Location"
                if (query.isNotEmpty() && query != "Your Current Location") {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
            }
        )
    )
}

@Composable
private fun PredictionItem(
    prediction: AutocompletePrediction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = "Location",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.1f))
                .padding(8.dp),
            tint = Color.Black
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = prediction.primaryText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = prediction.secondaryText,
                fontSize = 14.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AddressRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.1f))
                .padding(8.dp),
            tint = Color.Black
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}