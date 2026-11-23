package com.carife.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.carife.app.ui.compare.CompareScreen
import com.carife.app.ui.components.CarifeHairline
import com.carife.app.ui.detail.DetailScreen
import com.carife.app.ui.explore.ExploreScreen
import com.carife.app.ui.home.HomeScreen
import com.carife.app.ui.nav.Routes
import com.carife.app.ui.profile.AccountSettingsScreen
import com.carife.app.ui.profile.ProfileScreen
import com.carife.app.ui.teman.FriendsScreen
import com.carife.app.ui.teman.TemanScreen
import com.carife.app.ui.theme.CarifeOnPrimary
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifePrimaryFixed
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest

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
    BottomDestination(Routes.TEMAN, "Teman", Icons.Filled.Group, Icons.Outlined.Group),
)

@Composable
fun CarifeRoot(viewModel: CarifeViewModel = viewModel()) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    var locationRequested by rememberSaveable { mutableStateOf(false) }

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
        if (!locationRequested && !viewModel.locationProvider.hasPermission()) {
            locationRequested = true
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
        containerColor = MaterialTheme.colorScheme.background,
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
    Surface(color = CarifeSurfaceContainerLowest) {
        Column {
            CarifeHairline()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                destinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = {
                                    if (!selected) {
                                        navController.openTab(destination.route)
                                    }
                                },
                            )
                            .padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(min = 44.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) CarifePrimaryFixed else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (destination.route == Routes.COMPARE && compareCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = CarifePrimary,
                                            contentColor = CarifeOnPrimary,
                                        ) { Text(compareCount.toString()) }
                                    },
                                ) {
                                    Icon(
                                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = null,
                                        tint = if (selected) CarifePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = null,
                                    tint = if (selected) CarifePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) CarifePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private fun NavHostController.openTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
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
                onOpenSearch = {
                    viewModel.requestExploreEntry(ExploreEntry.SEARCH)
                    navController.openTab(Routes.EXPLORE)
                },
                onOpenExploreWithFilters = {
                    viewModel.requestExploreEntry(ExploreEntry.FILTERS)
                    navController.openTab(Routes.EXPLORE)
                },
                onOpenQuickFilter = {
                    viewModel.requestExploreEntry(ExploreEntry.QUICK_FILTER)
                    navController.openTab(Routes.EXPLORE)
                },
            )
        }

        composable(Routes.EXPLORE) {
            ExploreScreen(
                viewModel = viewModel,
                onOpenCafe = { cafeId -> navController.navigate(Routes.detail(cafeId)) },
                onOpenCompare = { navController.openTab(Routes.COMPARE) },
                onOpenHome = { navController.openTab(Routes.HOME) },
            )
        }

        composable(Routes.COMPARE) {
            CompareScreen(
                viewModel = viewModel,
                onOpenCafe = { cafeId -> navController.navigate(Routes.detail(cafeId)) },
                onPickCafes = { navController.openTab(Routes.EXPLORE) },
            )
        }

        composable(Routes.TEMAN) {
            TemanScreen(
                viewModel = viewModel,
                onOpenCafe = { cafeId -> navController.navigate(Routes.detail(cafeId)) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenFriends = { navController.navigate(Routes.FRIENDS) },
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenAccountSettings = { navController.navigate(Routes.ACCOUNT) },
            )
        }

        composable(Routes.FRIENDS) {
            FriendsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.ACCOUNT) {
            AccountSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.DETAIL) { entry ->
            val cafeId = entry.arguments?.getString(Routes.ARG_CAFE_ID).orEmpty()
            DetailScreen(
                viewModel = viewModel,
                cafeId = cafeId,
                onBack = { navController.popBackStack() },
                onOpenCompare = {
                    if (navController.previousBackStackEntry?.destination?.route == Routes.COMPARE) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Routes.COMPARE) { launchSingleTop = true }
                    }
                },
            )
        }
    }
}
