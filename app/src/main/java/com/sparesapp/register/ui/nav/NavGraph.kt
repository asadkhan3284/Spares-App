package com.sparesapp.register.ui.nav

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sparesapp.register.SparesApp
import com.sparesapp.register.auth.AuthManager
import com.sparesapp.register.data.RememberedLocation
import com.sparesapp.register.export.ExcelExporter
import com.sparesapp.register.ui.common.MainViewModelFactory
import com.sparesapp.register.ui.detail.DetailScreen
import com.sparesapp.register.ui.home.HomeScreen
import com.sparesapp.register.ui.picker.GraphBrowserScreen
import com.sparesapp.register.ui.picker.PickMode
import com.sparesapp.register.ui.scan.BarcodeScanScreen
import com.sparesapp.register.ui.settings.SettingsScreen
import com.sparesapp.register.ui.signin.SignInScreen

private object Routes {
    const val SIGN_IN = "signin"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val PICK_INVENTORY = "pick_inventory"
    const val PICK_IMAGES = "pick_images"
    const val SCAN = "scan"
    const val DETAIL = "detail/{mat}"
    fun detail(mat: String) = "detail/$mat"
}

@Composable
fun SparesNavGraph(app: SparesApp) {
    val navController = rememberNavController()
    val authState by app.authManager.authState.collectAsState()
    val vm = viewModel<com.sparesapp.register.ui.main.MainViewModel>(factory = MainViewModelFactory(app))
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )) { uri ->
        if (uri != null) {
            val rows = vm.filteredRows.value
            ExcelExporter.export(context, uri, rows)
        }
    }

    val startDestination = if (authState is AuthManager.AuthState.SignedIn) Routes.HOME else Routes.SIGN_IN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SIGN_IN) {
            SignInScreen(app) {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.SIGN_IN) { inclusive = true }
                }
            }
        }

        composable(Routes.HOME) {
            HomeScreen(
                app = app,
                vm = vm,
                onOpenRow = { mat -> navController.navigate(Routes.detail(mat)) },
                onOpenScan = { navController.navigate(Routes.SCAN) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onExport = {
                    exportLauncher.launch(ExcelExporter.suggestedFileName())
                },
            )
        }

        composable(
            Routes.DETAIL,
            arguments = listOf(navArgument("mat") { type = NavType.StringType })
        ) { backStackEntry ->
            val mat = backStackEntry.arguments?.getString("mat").orEmpty()
            DetailScreen(app = app, vm = vm, mat = mat, onBack = { navController.popBackStack() })
        }

        composable(Routes.SCAN) {
            BarcodeScanScreen(
                onCodeScanned = { code ->
                    val row = vm.findByCode(code)
                    if (row != null) {
                        navController.navigate(Routes.detail(row.mat)) {
                            popUpTo(Routes.HOME)
                        }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                app = app,
                vm = vm,
                onPickInventoryFile = { navController.navigate(Routes.PICK_INVENTORY) },
                onPickImagesFolder = { navController.navigate(Routes.PICK_IMAGES) },
                onSignedOut = {
                    navController.navigate(Routes.SIGN_IN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PICK_INVENTORY) {
            GraphBrowserScreen(
                app = app,
                mode = PickMode.INVENTORY_FILE,
                onPicked = { location: RememberedLocation ->
                    vm.setInventorySource(location)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PICK_IMAGES) {
            GraphBrowserScreen(
                app = app,
                mode = PickMode.IMAGES_FOLDER,
                onPicked = { location: RememberedLocation ->
                    vm.setImagesSource(location)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
