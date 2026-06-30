package io.github.icyoung

import io.github.icyoung.internal.axis.calculateTimeAxisAnchorSet
import io.github.icyoung.internal.axis.projectTimeAxisTicks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TimeAxisTicksTest {
    @Test
    fun splitsCurrentWindowIntoFourTicks() {
        val timestamps = timestamps(200)
        val anchorSet = calculateTimeAxisAnchorSet(
            candleTimestamps = timestamps,
            candleWidth = 9f,
            candleSpacing = 1f,
            xOffset = -1_000f,
            canvasWidth = 300f,
            labelCount = 4,
        )
        assertNotNull(anchorSet)
        val ticks = projectTimeAxisTicks(
            anchorSet = anchorSet,
            candleTimestamps = timestamps,
            candleWidth = 9f,
            candleSpacing = 1f,
            xOffset = -1_000f,
            canvasWidth = 300f,
            labelCount = 4,
            fadeAnimation = false,
        ).filter { it.x in 0f..300f }

        assertEquals(100L, anchorSet.originTimestamp)
        assertEquals(9, anchorSet.stepCandles)
        assertEquals(listOf(100, 109, 118, 127), ticks.map { it.index })
        assertTrue(ticks.all { it.x in 0f..300f })
        assertTrue(ticks.all { it.textAlpha == 1f })
    }

    @Test
    fun tickPositionsFollowSmallHorizontalOffset() {
        val timestamps = timestamps(200)
        val anchorSet = calculateTimeAxisAnchorSet(
            candleTimestamps = timestamps,
            candleWidth = 9f,
            candleSpacing = 1f,
            xOffset = -1_000f,
            canvasWidth = 300f,
            labelCount = 4,
        )
        assertNotNull(anchorSet)
        val first = projectTimeAxisTicks(
            anchorSet = anchorSet,
            candleTimestamps = timestamps,
            candleWidth = 9f,
            candleSpacing = 1f,
            xOffset = -1_000f,
            canvasWidth = 300f,
            labelCount = 4,
            fadeAnimation = false,
        )
        val second = projectTimeAxisTicks(
            anchorSet = anchorSet,
            candleTimestamps = timestamps,
            candleWidth = 9f,
            candleSpacing = 1f,
            xOffset = -995f,
            canvasWidth = 300f,
            labelCount = 4,
            fadeAnimation = false,
        )

        val firstByIndex = first.associateBy { it.index }
        val shared = second.first { it.index in firstByIndex }

        assertEquals(5f, shared.x - firstByIndex.getValue(shared.index).x)
    }

    @Test
    fun fadeAnimationControlsEdgeAlpha() {
        val timestamps = timestamps(200)
        val anchorSet = calculateTimeAxisAnchorSet(
            candleTimestamps = timestamps,
            candleWidth = 9f,
            candleSpacing = 1f,
            xOffset = -1_000f,
            canvasWidth = 300f,
            labelCount = 4,
        )
        assertNotNull(anchorSet)
        val withoutFade = projectTimeAxisTicks(
            anchorSet = anchorSet,
            candleTimestamps = timestamps,
            candleWidth = 9f,
            candleSpacing = 1f,
            xOffset = -1_000f,
            canvasWidth = 300f,
            labelCount = 4,
            fadeAnimation = false,
        )
        val withFade = projectTimeAxisTicks(
            anchorSet = anchorSet,
            candleTimestamps = timestamps,
            candleWidth = 9f,
            candleSpacing = 1f,
            xOffset = -1_000f,
            canvasWidth = 300f,
            labelCount = 4,
            fadeAnimation = true,
        )

        assertTrue(withoutFade.all { it.textAlpha == 1f })
        assertTrue(withFade.first().textAlpha < 1f)
        assertTrue(withFade.last().textAlpha < 1f)
    }

    @Test
    fun anchorSetSurvivesAppendAndPrepend() {
        val timestamps = timestamps(200)
        val anchorSet = calculateTimeAxisAnchorSet(
            candleTimestamps = timestamps,
            candleWidth = 9f,
            candleSpacing = 1f,
            xOffset = -1_000f,
            canvasWidth = 300f,
            labelCount = 4,
        )
        assertNotNull(anchorSet)
        val prepended = (-10L until 0L).toList() + timestamps
        val appended = timestamps + (200L until 210L).toList()
        val shiftedAnchorSet = anchorSet.copy(originIndex = anchorSet.originIndex + 10)

        val prependedTicks = projectTimeAxisTicks(
            anchorSet = shiftedAnchorSet,
            candleTimestamps = prepended,
            candleWidth = 9f,
            candleSpacing = 1f,
            xOffset = -1_100f,
            canvasWidth = 300f,
            labelCount = 4,
            fadeAnimation = false,
        )
        val stalePrependedTicks = projectTimeAxisTicks(
            anchorSet = anchorSet,
            candleTimestamps = prepended,
            candleWidth = 9f,
            candleSpacing = 1f,
            xOffset = -1_100f,
            canvasWidth = 300f,
            labelCount = 4,
            fadeAnimation = false,
        )
        val appendedTicks = projectTimeAxisTicks(
            anchorSet = anchorSet,
            candleTimestamps = appended,
            candleWidth = 9f,
            candleSpacing = 1f,
            xOffset = -1_000f,
            canvasWidth = 300f,
            labelCount = 4,
            fadeAnimation = false,
        )

        assertEquals(listOf(110, 119, 128, 137), prependedTicks.filter { it.x in 0f..300f }.map { it.index })
        assertTrue(stalePrependedTicks.isEmpty())
        assertEquals(listOf(100, 109, 118, 127), appendedTicks.filter { it.x in 0f..300f }.map { it.index })
    }

    @Test
    fun stableSequenceKeepsTicksAfterLongScroll() {
        val timestamps = timestamps(300)
        val anchorSet = calculateTimeAxisAnchorSet(
            candleTimestamps = timestamps,
            candleWidth = 9f,
            candleSpacing = 1f,
            xOffset = -1_000f,
            canvasWidth = 300f,
            labelCount = 4,
        )
        assertNotNull(anchorSet)

        val ticks = projectTimeAxisTicks(
            anchorSet = anchorSet,
            candleTimestamps = timestamps,
            candleWidth = 9f,
            candleSpacing = 1f,
            xOffset = -1_700f,
            canvasWidth = 300f,
            labelCount = 4,
            fadeAnimation = false,
        ).filter { it.x in 0f..300f }

        assertEquals(listOf(172, 181, 190, 199), ticks.map { it.index })
    }

    private fun timestamps(count: Int): List<Long> = List(count) { it.toLong() }
}
