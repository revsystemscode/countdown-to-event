package com.revsystems.countdowntoevent.ui.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Instant

/** Emits now, then on every [periodMillis] boundary (e.g. each whole second or minute). */
fun tickerFlow(
    periodMillis: Long,
    clock: Clock,
): Flow<Instant> =
    flow {
        while (true) {
            val now = clock.instant()
            emit(now)
            delay(periodMillis - now.toEpochMilli() % periodMillis)
        }
    }
