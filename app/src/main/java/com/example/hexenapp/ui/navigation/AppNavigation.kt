package com.example.hexenapp.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hexenapp.ui.screens.SplashScreen
import com.example.hexenapp.ui.screens.auth.ForgotPasswordScreen
import com.example.hexenapp.ui.screens.auth.LoginScreen
import com.example.hexenapp.ui.screens.auth.RegisterScreen
import com.example.hexenapp.ui.screens.auth.ResetPasswordConfirmScreen // Import layar baru
import com.example.hexenapp.ui.screens.items.AddEditItemScreen
import com.example.hexenapp.ui.screens.items.ItemListScreen
import com.example.hexenapp.ui.viewmodel.AuthViewModel

object AppRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    // Rute baru untuk konfirmasi reset password, dengan argumen oobCode
    const val RESET_PASSWORD_CONFIRM = "reset_password_confirm"
    const val ITEM_LIST = "item_list"
    const val ADD_EDIT_ITEM_BASE = "add_edit_item"
    const val ADD_EDIT_ITEM_ARG_ID = "itemId"
    const val ADD_EDIT_ITEM_ROUTE_TEMPLATE = "$ADD_EDIT_ITEM_BASE?$ADD_EDIT_ITEM_ARG_ID={$ADD_EDIT_ITEM_ARG_ID}"

    fun addEditItemRoute(itemId: String?): String {
        return if (itemId != null) {
            "$ADD_EDIT_ITEM_BASE?$ADD_EDIT_ITEM_ARG_ID=$itemId"
        } else {
            ADD_EDIT_ITEM_BASE
        }
    }
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val startDestination = AppRoutes.SPLASH

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(AppRoutes.SPLASH) {
            SplashScreen(
                navController = navController,
                onTimeout = {
                    // Logika navigasi dari splash tetap sama
                    if (currentUser != null) {
                        navController.navigate(AppRoutes.ITEM_LIST) {
                            popUpTo(AppRoutes.SPLASH) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        // Cek apakah ada deep link yang belum ditangani
                        // (biasanya MainActivity sudah menangani ini, tapi sebagai fallback)
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        if (currentRoute?.startsWith(AppRoutes.RESET_PASSWORD_CONFIRM) == false) {
                            navController.navigate(AppRoutes.LOGIN) {
                                popUpTo(AppRoutes.SPLASH) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        // Jika sudah di rute reset, biarkan MainActivity yang handle
                    }
                }
            )
        }

        composable(AppRoutes.LOGIN) {
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }

        composable(AppRoutes.REGISTER) {
            RegisterScreen(navController = navController, authViewModel = authViewModel)
        }

        composable(AppRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(navController = navController, authViewModel = authViewModel)
        }

        // Tambahkan composable untuk ResetPasswordConfirmScreen
        composable(
            route = "${AppRoutes.RESET_PASSWORD_CONFIRM}?oobCode={oobCode}",
            arguments = listOf(navArgument("oobCode") {
                type = NavType.StringType
                nullable = true // oobCode bisa null jika navigasi salah
            })
        ) { backStackEntry ->
            val oobCode = backStackEntry.arguments?.getString("oobCode")
            ResetPasswordConfirmScreen(
                navController = navController,
                oobCode = oobCode,
                authViewModel = authViewModel
            )
        }

        composable(AppRoutes.ITEM_LIST) {
            // ... (Logika penjagaan rute tetap sama) ...
            LaunchedEffect(currentUser) {
                if (currentUser == null) {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.ITEM_LIST) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            if (currentUser != null) { ItemListScreen(navController = navController) }
        }

        composable(
            route = AppRoutes.ADD_EDIT_ITEM_ROUTE_TEMPLATE, // Menggunakan template yang sudah ada
            arguments = listOf(
                navArgument(AppRoutes.ADD_EDIT_ITEM_ARG_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            // ... (Logika penjagaan rute tetap sama) ...
            LaunchedEffect(currentUser) {
                if (currentUser == null) {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            if (currentUser != null) {
                val itemId = backStackEntry.arguments?.getString(AppRoutes.ADD_EDIT_ITEM_ARG_ID)
                AddEditItemScreen(navController = navController, itemId = itemId)
            }
        }
    }
}