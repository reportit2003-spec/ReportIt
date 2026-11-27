package com.example.reportit.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object SubmitComplaint : Screen("submit_complaint/{complaintId}") {
        fun createRoute(complaintId: String? = null): String {
            return if (complaintId != null) "submit_complaint/$complaintId"
            else "submit_complaint/new"
        }
    }
    data object Settings : Screen("settings")
}