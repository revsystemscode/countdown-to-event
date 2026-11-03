package com.revsystems.countdowntoevent.ui.screens.home

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.revsystems.countdowntoevent.R
import com.revsystems.countdowntoevent.domain.EventClassifier
import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.ui.theme.CountdownTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun str(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun stateWith(vararg events: Event): HomeUiState {
        val c = EventClassifier.classify(events.toList(), Instant.now(), ZoneId.systemDefault())
        return HomeUiState(isLoading = false, isEmpty = events.isEmpty(), upcoming = c.upcoming, reached = c.reached)
    }

    @Test
    fun emptyStateTabsAndAdd() {
        var addClicked = false
        composeRule.setContent {
            CountdownTheme {
                HomeContent(stateWith(), { addClicked = true }, {}, {}, {}, {}, {})
            }
        }

        composeRule.onNodeWithText(str(R.string.home_empty_title)).assertExists()
        composeRule.onNodeWithText(str(R.string.tab_upcoming)).assertIsSelected()
        composeRule.onNodeWithContentDescription(str(R.string.action_add_event)).performClick()
        assertTrue(addClicked)

        composeRule.onNodeWithText(str(R.string.tab_reached)).performClick().assertIsSelected()
        composeRule.onNodeWithText(str(R.string.reached_empty)).assertExists()
    }

    @Test
    fun cardOpensAndSwipeDeletesWithUndo() {
        val trip = Event(id = 1, title = "Trip to Lisbon", startDate = LocalDate.now().plusDays(5))
        // Behaves like the real ViewModel: the list reflects deletes and restores.
        val events = mutableStateListOf(trip)
        var opened: Long? = null
        val deleted = mutableListOf<Long>()
        composeRule.setContent {
            CountdownTheme {
                HomeContent(
                    state = stateWith(*events.toTypedArray()),
                    onAddEvent = {},
                    onOpenEvent = { opened = it },
                    onEditEvent = {},
                    onDelete = {
                        deleted += it.id
                        events.remove(it)
                    },
                    onUndoDelete = { events.add(it) },
                    onDeleteFinal = {},
                )
            }
        }

        composeRule.onNodeWithText("Trip to Lisbon").performClick()
        assertEquals(1L, opened)

        composeRule.onNodeWithText("Trip to Lisbon").performTouchInput { swipeLeft() }
        composeRule.waitUntil(3_000) { deleted.isNotEmpty() }
        composeRule.onNodeWithText("Trip to Lisbon").assertDoesNotExist()

        composeRule.onNodeWithText(str(R.string.action_undo)).performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.onNodeWithText("Trip to Lisbon").assertExists()
        // Regression: a restored card must not dismiss itself again.
        assertEquals(listOf(1L), deleted)
    }
}
