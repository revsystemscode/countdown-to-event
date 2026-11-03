package com.revsystems.countdowntoevent.ui.screens.editor

import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revsystems.countdowntoevent.R
import com.revsystems.countdowntoevent.domain.EventValidationError
import com.revsystems.countdowntoevent.domain.EventValidator
import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import com.revsystems.countdowntoevent.ui.components.EventCard
import com.revsystems.countdowntoevent.ui.format.labelRes
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val EMOJI_CHOICES = listOf("🎂", "✈️", "❤️", "🎉", "🏖️", "🎄", "🎓", "🎵", "👶", "💍", "⚽", "🏠")

@Composable
fun EventEditorScreen(
    onClose: () -> Unit,
    viewModel: EventEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) { if (state.isDone) onClose() }

    // System photo picker: no storage permission needed.
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) viewModel.onImagePicked(uri.toString())
        }

    EventEditorContent(
        state = state,
        onClose = onClose,
        onSave = viewModel::save,
        onTitleChange = viewModel::onTitleChange,
        onEmojiChange = viewModel::onEmojiChange,
        onStartDateChange = viewModel::onStartDateChange,
        onAllDayChange = viewModel::onAllDayChange,
        onTimeChange = viewModel::onTimeChange,
        onMultiDayChange = viewModel::onMultiDayChange,
        onEndDateChange = viewModel::onEndDateChange,
        onRepeatChange = viewModel::onRepeatChange,
        onBackgroundChange = viewModel::onBackgroundChange,
        onPickImage = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
    )
}

private enum class PickerDialog { START_DATE, TIME, END_DATE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventEditorContent(
    state: EditorUiState,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onTitleChange: (String) -> Unit,
    onEmojiChange: (String?) -> Unit,
    onStartDateChange: (LocalDate) -> Unit,
    onAllDayChange: (Boolean) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onMultiDayChange: (Boolean) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    onRepeatChange: (RepeatRule) -> Unit,
    onBackgroundChange: (EventBackground) -> Unit,
    onPickImage: () -> Unit,
) {
    var dialog by rememberSaveable { mutableStateOf<PickerDialog?>(null) }
    val draft = state.draft
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.editor_title_new else R.string.editor_title_edit)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !state.isSaving && !state.isLoading && !state.isImportingImage) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val placeholderTitle = stringResource(R.string.editor_preview_title)
            EventCard(
                event = state.preview.event.copy(title = draft.title.ifBlank { placeholderTitle }),
                countdown = state.preview.countdown,
            )

            TitleField(draft.title, state.errors, onTitleChange)

            Section(stringResource(R.string.field_emoji)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = draft.emoji == null,
                        onClick = { onEmojiChange(null) },
                        label = { Text(stringResource(R.string.emoji_none)) },
                    )
                    EMOJI_CHOICES.forEach { emoji ->
                        FilterChip(
                            selected = draft.emoji == emoji,
                            onClick = { onEmojiChange(if (draft.emoji == emoji) null else emoji) },
                            label = { Text(emoji, style = MaterialTheme.typography.titleMedium) },
                        )
                    }
                }
            }

            Section(stringResource(R.string.field_when)) {
                SwitchRow(stringResource(R.string.field_all_day), draft.startTime == null, onAllDayChange)
                PickerRow(
                    icon = Icons.Outlined.CalendarToday,
                    label = stringResource(R.string.field_date),
                    value = dateFormat.format(draft.startDate),
                    onClick = { dialog = PickerDialog.START_DATE },
                )
                draft.startTime?.let { time ->
                    PickerRow(
                        icon = Icons.Outlined.Schedule,
                        label = stringResource(R.string.field_time),
                        value = timeFormat.format(time),
                        onClick = { dialog = PickerDialog.TIME },
                    )
                }
                SwitchRow(stringResource(R.string.field_multi_day), draft.endDate != null, onMultiDayChange)
                draft.endDate?.let { end ->
                    PickerRow(
                        icon = Icons.Outlined.CalendarToday,
                        label = stringResource(R.string.field_end_date),
                        value = dateFormat.format(end),
                        onClick = { dialog = PickerDialog.END_DATE },
                    )
                }
                if (EventValidationError.END_BEFORE_START in state.errors) {
                    ErrorText(stringResource(R.string.error_end_before_start))
                }
            }

            Section(stringResource(R.string.field_repeat)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeatRule.entries.forEach { rule ->
                        FilterChip(
                            selected = draft.repeat == rule,
                            onClick = { onRepeatChange(rule) },
                            label = { Text(stringResource(if (rule == RepeatRule.NONE) R.string.repeat_never else rule.labelRes())) },
                        )
                    }
                }
            }

            Section(stringResource(R.string.field_background)) {
                BackgroundPicker(
                    selected = draft.background,
                    isImporting = state.isImportingImage,
                    imageError = state.imageError,
                    onSelect = onBackgroundChange,
                    onPickImage = onPickImage,
                )
            }
        }
    }

    when (dialog) {
        PickerDialog.START_DATE ->
            DatePickerModal(draft.startDate, minDate = null, onDismiss = { dialog = null }) {
                onStartDateChange(it)
                dialog = null
            }
        PickerDialog.END_DATE ->
            DatePickerModal(draft.endDate ?: draft.startDate, minDate = draft.startDate, onDismiss = { dialog = null }) {
                onEndDateChange(it)
                dialog = null
            }
        PickerDialog.TIME ->
            TimePickerModal(draft.startTime ?: LocalTime.NOON, onDismiss = { dialog = null }) {
                onTimeChange(it)
                dialog = null
            }
        null -> Unit
    }
}

@Composable
private fun TitleField(
    title: String,
    errors: Set<EventValidationError>,
    onTitleChange: (String) -> Unit,
) {
    val length = title.trim().let { it.codePointCount(0, it.length) }
    val error =
        when {
            EventValidationError.BLANK_TITLE in errors -> stringResource(R.string.error_title_blank)
            EventValidationError.TITLE_TOO_LONG in errors -> stringResource(R.string.error_title_long, EventValidator.MAX_TITLE_LENGTH)
            else -> null
        }
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text(stringResource(R.string.field_title)) },
        singleLine = true,
        isError = error != null,
        supportingText = { Text(error ?: "$length / ${EventValidator.MAX_TITLE_LENGTH}") },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        content()
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Switch) { onCheckedChange(!checked) }
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun PickerRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClickLabel = label, onClick = onClick)
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

// Material's date picker works in UTC millis at midnight.
private fun LocalDate.toPickerMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toPickerDate() = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initial: LocalDate,
    minDate: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val minMillis = minDate?.toPickerMillis()
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initial.toPickerMillis(),
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = minMillis == null || utcTimeMillis >= minMillis
                },
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { pickerState.selectedDateMillis?.let { onConfirm(it.toPickerDate()) } ?: onDismiss() }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerModal(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val context = LocalContext.current
    val pickerState =
        rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        text = { TimePicker(state = pickerState) },
    )
}
