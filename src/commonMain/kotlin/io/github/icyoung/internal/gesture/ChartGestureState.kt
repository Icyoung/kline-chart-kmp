package io.github.icyoung.internal.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

internal class ChartGestureState(
    private val scope: CoroutineScope,
    private val density: Density,
    private val onLoadMore: () -> Unit,
) {
    private val activationDelayMs = 300L
    private val velocityTracker = VelocityTracker()
    private val areaTop = LinkedHashMap<String, Float>()
    private val areaHeight = LinkedHashMap<String, Float>()
    private var inertiaJob: Job? = null
    private var originAreaId: String = MainAreaId
    private var requestedLoadMore = false
    private var loadingMore = false

    var zoom by mutableStateOf(1f)
        private set
    var xOffset by mutableStateOf(0f)
        private set
    var canvasSize by mutableStateOf(Size.Zero)
        private set
    var isInitialPosition by mutableStateOf(true)
        private set
    var showCrosshair by mutableStateOf(false)
        private set
    var crosshairPosition by mutableStateOf(Offset.Zero)
        private set
    var crosshairAbsoluteY by mutableStateOf(0f)
        private set
    var crosshairAreaId by mutableStateOf(MainAreaId)
        private set

    fun updateCanvasSize(size: Size) {
        canvasSize = size
    }

    fun updateArea(areaId: String, topY: Float, height: Float) {
        areaTop[areaId] = topY
        areaHeight[areaId] = height
    }

    fun areaTop(areaId: String): Float = areaTop[areaId] ?: 0f

    fun areaHeight(areaId: String): Float = areaHeight[areaId] ?: canvasSize.height

    fun setInitialPosition(
        candleCount: Int,
        candleWidth: Float,
        candleSpacing: Float,
        calculateBounds: (Float, Float, Float) -> Pair<Float, Float>,
    ) {
        if (!isInitialPosition || canvasSize == Size.Zero || candleCount < 10) return

        val step = candleWidth + candleSpacing
        val targetScreenPosition = canvasSize.width * 0.75f
        val totalWidth = candleCount * step
        xOffset = if (totalWidth > targetScreenPosition) {
            val targetOffset = targetScreenPosition - (candleCount - 1) * step - candleWidth / 2
            val (leftBound, rightBound) = calculateBounds(candleWidth, candleSpacing, canvasSize.width)
            targetOffset.coerceIn(leftBound, rightBound)
        } else {
            (canvasSize.width - totalWidth) / 2f
        }
        isInitialPosition = false
    }

    fun onHistoricalDataLoaded(previousCount: Int, currentCount: Int, candleWidth: Float, candleSpacing: Float) {
        if (previousCount > 0 && currentCount > previousCount) {
            xOffset += (currentCount - previousCount) * (candleWidth + candleSpacing)
            requestedLoadMore = false
            loadingMore = false
        }
    }

    fun clampOffset(leftBound: Float, rightBound: Float) {
        xOffset = xOffset.coerceIn(leftBound, rightBound)
    }

    fun handleZoom(
        zoomFactor: Float,
        baseCandleWidth: Float,
        baseCandleSpacing: Float,
        candleCount: Int,
        focusX: Float?,
        minCandleWidth: Float,
        maxCandleWidth: Float,
        calculateBounds: (Float, Float, Float) -> Pair<Float, Float>,
    ) {
        if (zoomFactor == 1f || canvasSize == Size.Zero || candleCount == 0) return
        stopInertia()

        val oldZoom = zoom
        val adjustedZoomFactor = 1f + (zoomFactor - 1f) * 1.5f
        val rawZoom = (zoom * adjustedZoomFactor).coerceIn(0.5f, 5f)
        val newCandleWidth = (baseCandleWidth * rawZoom).coerceIn(minCandleWidth, maxCandleWidth)
        val newZoom = newCandleWidth / baseCandleWidth
        if (newZoom == oldZoom) return

        val oldCandleWidth = baseCandleWidth * oldZoom
        val centerX = (focusX ?: canvasSize.width / 2f).coerceIn(0f, canvasSize.width)
        val oldStep = oldCandleWidth + baseCandleSpacing
        val anchorIndex = ((centerX - xOffset - oldCandleWidth / 2) / oldStep).toInt()
            .coerceIn(0, candleCount - 1)
        val newStep = newCandleWidth + baseCandleSpacing
        val newXOffset = centerX - (anchorIndex * newStep + newCandleWidth / 2)
        val (leftBound, rightBound) = calculateBounds(newCandleWidth, baseCandleSpacing, canvasSize.width)

        xOffset = newXOffset.coerceIn(leftBound, rightBound)
        zoom = newZoom
        isInitialPosition = false
    }

    fun nearestIndex(x: Float, itemCount: Int, candleWidth: Float, candleSpacing: Float): Int {
        if (itemCount == 0) return -1
        val step = candleWidth + candleSpacing
        return ((x - xOffset - candleWidth / 2) / step).toInt().coerceIn(0, itemCount - 1)
    }

    fun hideCrosshair() {
        showCrosshair = false
        crosshairPosition = Offset.Zero
        crosshairAbsoluteY = 0f
        crosshairAreaId = MainAreaId
        originAreaId = MainAreaId
    }

    private fun startCrosshair(position: Offset, areaId: String) {
        showCrosshair = true
        crosshairPosition = position
        crosshairAreaId = areaId
        originAreaId = areaId
        crosshairAbsoluteY = areaTop(areaId) + position.y
        stopInertia()
    }

    private fun updateCrosshair(position: Offset) {
        if (!showCrosshair) return
        crosshairPosition = position
        val absY = areaTop(originAreaId) + position.y
        crosshairAbsoluteY = absY
        for ((id, top) in areaTop) {
            val height = areaHeight[id] ?: continue
            if (absY >= top && absY < top + height) {
                crosshairAreaId = id
                return
            }
        }
    }

    private fun stopInertia() {
        inertiaJob?.cancel()
        inertiaJob = null
    }

    private fun startInertia(initialVelocity: Float, leftBound: Float, rightBound: Float) {
        inertiaJob?.cancel()
        inertiaJob = scope.launch {
            val decay = splineBasedDecay<Float>(density)
            val animatable = Animatable(xOffset)
            animatable.updateBounds(leftBound, rightBound)
            animatable.animateDecay(initialVelocity, decay) {
                xOffset = value
            }
            xOffset = animatable.value.coerceIn(leftBound, rightBound)
        }
    }

    suspend fun PointerInputScope.handleGestures(
        areaId: String,
        scrollBounds: () -> Pair<Float, Float>,
        onTap: (areaId: String, position: Offset) -> Boolean = { _, _ -> false },
    ) {
        awaitEachGesture {
            val down = awaitFirstDown()
            if (showCrosshair) {
                hideCrosshair()
                down.consume()
                return@awaitEachGesture
            }

            velocityTracker.resetTracking()
            velocityTracker.addPosition(down.uptimeMillis, down.position)

            var isDragging = false
            var isLongPress = false
            var isVerticalScroll = false
            var latestPosition = down.position
            val pointerId = down.id
            val longPressJob = scope.launch {
                delay(activationDelayMs)
                if (!isDragging && !isVerticalScroll && !isLongPress) {
                    isLongPress = true
                    startCrosshair(latestPosition, areaId)
                }
            }

            try {
                var pointer: PointerInputChange = down
                do {
                    val event = awaitPointerEvent()
                    pointer = event.changes.firstOrNull { it.id == pointerId }
                        ?: event.changes.firstOrNull()
                        ?: break
                    latestPosition = pointer.position
                    if (!pointer.pressed) break

                    if (isLongPress) {
                        updateCrosshair(pointer.position)
                        pointer.consume()
                    } else {
                        val delta = pointer.position - down.position
                        if (delta.getDistance() > 10f) {
                            longPressJob.cancel()
                            if (abs(delta.y) > abs(delta.x)) {
                                isVerticalScroll = true
                                break
                            }
                            isDragging = true
                            stopInertia()
                        }

                        if (isDragging) {
                            val (leftBound, rightBound) = scrollBounds()
                            val nextOffset = (xOffset + pointer.positionChange().x).coerceIn(leftBound, rightBound)
                            xOffset = nextOffset
                            velocityTracker.addPosition(pointer.uptimeMillis, pointer.position)
                            isInitialPosition = false

                            if (pointer.positionChange().x > 0 && !loadingMore && !requestedLoadMore) {
                                val threshold = canvasSize.width * 0.2f
                                if (nextOffset > -threshold) {
                                    requestedLoadMore = true
                                    loadingMore = true
                                    onLoadMore()
                                }
                            }
                            pointer.consume()
                        }
                    }
                } while (pointer.pressed)

                if (isVerticalScroll) return@awaitEachGesture
                when {
                    isDragging -> {
                        val velocity = velocityTracker.calculateVelocity()
                        if (abs(velocity.x) > 40f) {
                            val (leftBound, rightBound) = scrollBounds()
                            startInertia(
                                initialVelocity = velocity.x.coerceIn(-6000f, 6000f) * 1.1f,
                                leftBound = leftBound,
                                rightBound = rightBound,
                            )
                        }
                    }
                    !isLongPress -> {
                        if (!onTap(areaId, latestPosition)) {
                            startCrosshair(latestPosition, areaId)
                        }
                    }
                }
            } finally {
                longPressJob.cancel()
            }
        }
    }

    companion object {
        const val MainAreaId = "MAIN"
    }
}
