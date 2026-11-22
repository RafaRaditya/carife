package com.carife.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.carife.app.ui.compare.CompareScreen
import com.carife.app.ui.detail.DetailScreen
import com.carife.app.ui.explore.ExploreScreen
import com.carife.app.ui.home.HomeScreen
import com.carife.app.ui.nav.Routes
import com.carife.app.ui.profile.ProfileScreen

private data class BottomDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val destinations = listOf(
    BottomDestination(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomDestination(Routes.EXPLORE, "Peta", Icons.Filled.Map, Icons.Outlined.Map),
    BottomDestination(Routes.COMPARE, "Bandingkan", Icons.Filled.CompareArrows, Icons.Outlined.CompareArrows),
    BottomDestination(Routes.PROFILE, "Teman", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle),
)

@Composable
fun CarifeRoot(viewModel: CarifeViewModel = viewModel()) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val appState by viewModel.appState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result.values.any { it }
        viewModel.onLocationPermissionResult(granted)
        if (!granted) {
            viewModel.emitMessage("Tanpa izin lokasi, jarak dihitung dari pusat kota.")
        }
    }

    LaunchedEffect(Unit) {
        if (!viewModel.locationProvider.hasPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message.text)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in destinations.map { it.route }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                CarifeBottomBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    compareCount = appState.compareIds.size,
                )
            }
        },
    ) { padding ->
        CarifeNavHost(
            navController = navController,
            viewModel = viewModel,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun CarifeBottomBar(
    navController: NavHostController,
    currentRoute: String?,
    compareCount: Int,
) {
    NavigationBar {
        destinations.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(destination.route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    if (destination.route == Routes.COMPARE && compareCount > 0) {
                        BadgedBox(badge = { Badge { Text(compareCount.toString()) } }) {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = null,
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = null,
                        )
                    }
                },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
private fun CarifeNavHost(
    navController: NavHostController,
    viewModel: CarifeViewModel,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onOpenCafe = { cafeId -> navController.navigate(Routes.detail(cafeId)) },
                onOpenExplore = { navController.navigate(Routes.EXPLORE) { launchSingleTop = true } },
                onOpenExploreWithFilters = {
                    viewModel.openFilterPanel()
                    navController.navigate(Routes.EXPLORE) { launchSingleTop = true }
                },
            )
        }

        composable(Routes.EXPLORE) {
            ExploreScreen(
                viewModel = viewModel,
                onOpenCafe = { cafeId -> navController.navigate(Routes.detail(cafeId)) },
                onOpenCompare = { navController.navigate(Routes.COMPARE) { launchSingleTop = true } },
            )
        }

        composable(Routes.COMPARE) {
            CompareScreen(
                viewModel = viewModel,
                onOpenCafe = { cafeId -> navController.navigate(Routes.detail(cafeId)) },
                onPickCafes = { navController.navigate(Routes.EXPLORE) { launchSingleTop = true } },
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                viewModel = viewModel,
                onOpenCafe = { cafeId -> navController.navigate(Routes.detail(cafeId)) },
            )
        }

        composable(Routes.DETAIL) { entry ->
            val cafeId = entry.arguments?.getString(Routes.ARG_CAFE_ID).orEmpty()
            DetailScreen(
                viewModel = viewModel,
                cafeId = cafeId,
                onBack = { navController.popBackStack() },
                onOpenCompare = { navController.navigate(Routes.COMPARE) { launchSingleTop = true } },
            )
        }
    }
}
