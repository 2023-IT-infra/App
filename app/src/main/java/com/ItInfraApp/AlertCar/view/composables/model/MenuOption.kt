package com.ItInfraApp.AlertCar.view.composables.model

import androidx.compose.ui.graphics.vector.ImageVector

data class MenuOption(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit
)