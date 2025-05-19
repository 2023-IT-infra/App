package com.ItInfraApp.AlertCar.view.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ItInfraApp.AlertCar.view.composables.model.MenuOption

@Composable
fun MainDropDownMenuButton(
    options: List<MenuOption>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(
        onClick = { expanded = true },
        modifier = modifier
    ) {
        Icon(Icons.Default.MoreVert, contentDescription = "더보기 메뉴")
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        options.forEach { item ->
            DropdownMenuItem(
                text = { Text(item.label) },
                onClick = {
                    expanded = false
                    item.onClick()
                },
                leadingIcon = item.icon?.let { icon ->
                    { Icon(icon, contentDescription = item.label) }
                }
            )
        }
    }
}

@Preview
@Composable
fun MainDropDownMenuButtonPreview() {
}