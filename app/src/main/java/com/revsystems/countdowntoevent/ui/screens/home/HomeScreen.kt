package com.revsystems.countdowntoevent.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revsystems.countdowntoevent.R
import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.ui.components.EventCard
import com.revsystems.countdowntoevent.ui.permissions.rememberNotificationAccess
import com.revsystems.countdowntoevent.ui.share.shareEvent
import com.revsystems.countdowntoevent.ui.theme.CountdownTheme
import kotlinx.coroutines.launch

enum class HomeTab(
    val labelRes: Int,
) {
    Upcoming(R.string.tab_upcoming),
    Reached(R.string.tab_reached),
}

@Composable
fun HomeScreen(
    onAddEvent: () -> Unit,
    onOpenEvent: (Long) -> Unit,
    onEditEvent: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val access =
        rememberNotificationAccess(
            wasAskedBefore = viewModel::notificationPermissionAsked,
            onAsked = viewModel::markNotificationPermissionAsked,
            // A "no" in the system dialog counts as "not now": don't keep asking from Home.
            onResult = { granted -> if (!granted) viewModel.dismissNotificationPrompt() },
        )
    HomeContent(
        state = state,
        onAddEvent = onAddEvent,
        onOpenEvent = onOpenEvent,
        onEditEvent = onEditEvent,
        onDelete = viewModel::delete,
        onUndoDelete = viewModel::restore,
        onDeleteFinal = viewModel::discardDeleted,
        modifier = modifier,
        showNotificationPrompt = state.mayPromptForNotifications && !state.isEmpty && !access.notificationsAllowed,
        onEnableNotifications = access.requestNotifications,
        onDismissNotificationPrompt = viewModel::dismissNotificationPrompt,
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onAddEvent: () -> Unit,
    onOpenEvent: (Long) -> Unit,
    onEditEvent: (Long) -> Unit,
    onDelete: (Event) -> Unit,
    onUndoDelete: (Event) -> Unit,
    onDeleteFinal: (Event) -> Unit,
    modifier: Modifier = Modifier,
    showNotificationPrompt: Boolean = false,
    onEnableNotifications: () -> Unit = {},
    onDismissNotificationPrompt: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.Upcoming) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val deletedMessage = stringResource(R.string.event_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    val editLabel = stringResource(R.string.action_edit)
    val deleteLabel = stringResource(R.string.action_delete)

    fun deleteWithUndo(event: Event) {
        onDelete(event)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(deletedMessage, actionLabel = undoLabel, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) onUndoDelete(event) else onDeleteFinal(event)
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            HomeTabs(selectedTab, onSelect = { selectedTab = it })

            if (!state.isLoading) {
                val items = if (selectedTab == HomeTab.Upcoming) state.upcoming else state.reached
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (selectedTab == HomeTab.Upcoming && state.isEmpty) {
                        item(key = "welcome") { Welcome() }
                    }
                    if (selectedTab == HomeTab.Upcoming && showNotificationPrompt) {
                        item(key = "notification-prompt") {
                            NotificationPrompt(
                                onEnable = onEnableNotifications,
                                onDismiss = onDismissNotificationPrompt,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    items(items, key = { it.event.id }) { item ->
                        SwipeToDelete(onDelete = { deleteWithUndo(item.event) }, modifier = Modifier.animateItem()) {
                            EventCard(
                                event = item.event,
                                countdown = item.countdown,
                                onClick = { onOpenEvent(item.event.id) },
                                onLongClick = { onEditEvent(item.event.id) },
                                onShare = { scope.launch { shareEvent(context, item.event, item.countdown) } },
                                accessibilityActions =
                                    listOf(
                                        CustomAccessibilityAction(editLabel) {
                                            onEditEvent(item.event.id)
                                            true
                                        },
                                        CustomAccessibilityAction(deleteLabel) {
                                            deleteWithUndo(item.event)
                                            true
                                        },
                                    ),
                            )
                        }
                    }
                    if (selectedTab == HomeTab.Upcoming) {
                        item(key = "add") { AddEventCard(onClick = onAddEvent, modifier = Modifier.animateItem()) }
                    } else if (items.isEmpty()) {
                        item(key = "empty-reached") { EmptyReached() }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun HomeTabs(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
) {
    PrimaryTabRow(
        selectedTabIndex = selected.ordinal,
        modifier = Modifier.padding(horizontal = 32.dp),
        containerColor = MaterialTheme.colorScheme.background,
        divider = {},
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selected.ordinal, matchContentSize = true),
                color = MaterialTheme.colorScheme.primary,
                width = 40.dp,
            )
        },
    ) {
        HomeTab.entries.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                selectedContentColor = MaterialTheme.colorScheme.onBackground,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    // One line at any font size: shrink rather than wrap "Alcanzad-os".
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = MaterialTheme.typography.titleMedium.fontSize),
                    )
                },
            )
        }
    }
}

private const val DISMISS_FRACTION = 0.4f

@Composable
private fun SwipeToDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // Plain remember, not rememberSwipeToDismissBoxState(): that one is saveable, so an event
    // restored by "Undo" (same list key) would come back already dismissed and delete itself again.
    val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled) { distance -> distance * DISMISS_FRACTION } }
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        onDismiss = { onDelete() },
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 28.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onError)
            }
        },
    ) { content() }
}

@Composable
private fun NotificationPrompt(
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(R.string.prompt_notifications_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Text(
            text = stringResource(R.string.prompt_notifications_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(top = 6.dp, end = 8.dp),
        )
        // Primary-on-container teal is under 4.5:1, so buttons use the container's text color.
        val buttonColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss, colors = buttonColors) { Text(stringResource(R.string.action_not_now)) }
            TextButton(onClick = onEnable, colors = buttonColors) {
                Text(stringResource(R.string.action_enable), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Welcome() {
    Column(Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AddEventCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.action_add_event)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(184.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyReached() {
    Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.reached_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeEmptyPreview() {
    CountdownTheme {
        HomeContent(
            state = HomeUiState(isLoading = false, isEmpty = true),
            onAddEvent = {},
            onOpenEvent = {},
            onEditEvent = {},
            onDelete = {},
            onUndoDelete = {},
            onDeleteFinal = {},
        )
    }
}
