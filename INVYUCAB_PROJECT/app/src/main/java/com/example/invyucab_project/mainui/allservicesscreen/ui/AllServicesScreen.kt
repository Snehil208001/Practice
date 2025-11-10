package com.example.invyucab_project.mainui.allservicesscreen.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
// ✅✅✅ START OF FIX ✅✅✅
// Added the correct import for 'AppBottomNavigation'
import com.example.invyucab_project.core.utils.navigationsbar.AppBottomNavigation
// ✅✅✅ END OF FIX ✅✅✅

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllServicesScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "All Services",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00BFA5), // Assuming CabMintGreen
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            // ✅✅✅ START OF FIX ✅✅✅
            // Changed the name back to the correct one
            AppBottomNavigation(navController = navController, selectedItem = "services")
            // ✅✅✅ END OF FIX ✅✅✅
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Text("All Services Screen")
        }
    }
}