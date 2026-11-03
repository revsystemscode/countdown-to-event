package com.revsystems.countdowntoevent.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revsystems.countdowntoevent.R
import com.revsystems.countdowntoevent.ui.theme.CountdownTheme

@Composable
fun AppBottomBar(
    homeSelected: Boolean,
    settingsSelected: Boolean,
    onHome: () -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemColors =
        NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onBackground,
            selectedTextColor = MaterialTheme.colorScheme.onBackground,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        NavigationBarItem(
            selected = homeSelected,
            onClick = onHome,
            icon = { Icon(if (homeSelected) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_home)) },
            colors = itemColors,
        )

        // Center "+" action, styled as a pill like a primary button.
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            FilledIconButton(
                onClick = onAdd,
                modifier = Modifier.size(width = 64.dp, height = 44.dp),
                shape = MaterialTheme.shapes.medium,
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_event))
            }
        }

        NavigationBarItem(
            selected = settingsSelected,
            onClick = onSettings,
            icon = {
                Icon(if (settingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = null)
            },
            label = { Text(stringResource(R.string.nav_settings)) },
            colors = itemColors,
        )
    }
}

@Preview
@Composable
private fun AppBottomBarPreview() {
    CountdownTheme {
        AppBottomBar(homeSelected = true, settingsSelected = false, onHome = {}, onAdd = {}, onSettings = {})
    }
}
