package com.whitestore.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Official Material 3 Expressive motion for full-screen dialog / container transform:
 * Fast spatial spring animation with emphasized easing curve.
 */
private val M3EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
private val M3EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
private val M3EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

@Composable
fun FullscreenImageViewer(
    images: List<String>,
    initialIndex: Int = 0,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (images.isEmpty()) return

    BackHandler(enabled = isVisible) {
        onDismiss()
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, images.lastIndex),
        pageCount = { images.size }
    )

    // Sync initial index whenever visibility opens
    LaunchedEffect(isVisible, initialIndex) {
        if (isVisible) {
            pagerState.scrollToPage(initialIndex.coerceIn(0, images.lastIndex))
        }
    }

    var showControls by remember { mutableStateOf(true) }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        isCurrentPageZoomed = false
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 280, easing = LinearEasing)
        ) + scaleIn(
            initialScale = 0.88f,
            animationSpec = tween(durationMillis = 380, easing = M3EmphasizedDecelerate)
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 200, easing = LinearEasing)
        ) + scaleOut(
            targetScale = 0.90f,
            animationSpec = tween(durationMillis = 250, easing = M3EmphasizedAccelerate)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !isCurrentPageZoomed
            ) { page ->
                ZoomableImage(
                    imageUrl = images[page],
                    isCurrentPage = pagerState.currentPage == page,
                    onZoomChanged = { zoomed ->
                        if (pagerState.currentPage == page) {
                            isCurrentPageZoomed = zoomed
                        }
                    },
                    onToggleControls = { showControls = !showControls }
                )
            }

            // Top Bar Overlay
            if (showControls) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White
                        )
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${images.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    imageUrl: String,
    isCurrentPage: Boolean,
    onZoomChanged: (Boolean) -> Unit,
    onToggleControls: () -> Unit
) {
    var rawScale by remember { mutableFloatStateOf(1f) }
    var rawOffset by remember { mutableStateOf(Offset.Zero) }

    // Material 3 Expressive motion spec for double-tap zoom
    val animatedScale by animateFloatAsState(
        targetValue = rawScale,
        animationSpec = tween(
            durationMillis = 350,
            easing = M3EmphasizedEasing
        ),
        label = "zoom_scale"
    )

    val animatedOffset by animateOffsetAsState(
        targetValue = rawOffset,
        animationSpec = tween(
            durationMillis = 350,
            easing = M3EmphasizedEasing
        ),
        label = "pan_offset"
    )

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            rawScale = 1f
            rawOffset = Offset.Zero
            onZoomChanged(false)
        }
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (rawScale * zoomChange).coerceIn(1f, 5f)
        rawScale = newScale
        onZoomChanged(newScale > 1.05f)

        if (newScale > 1f) {
            val maxPanX = (newScale - 1f) * 1000f
            val maxPanY = (newScale - 1f) * 1500f
            rawOffset = Offset(
                x = (rawOffset.x + panChange.x * newScale).coerceIn(-maxPanX, maxPanX),
                y = (rawOffset.y + panChange.y * newScale).coerceIn(-maxPanY, maxPanY)
            )
        } else {
            rawOffset = Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = {
                        if (rawScale > 1.05f) {
                            rawScale = 1f
                            rawOffset = Offset.Zero
                            onZoomChanged(false)
                        } else {
                            rawScale = 2.5f
                            onZoomChanged(true)
                        }
                    }
                )
            }
            .transformable(
                state = transformState,
                canPan = { rawScale > 1.05f },
                enabled = true
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Скриншот на весь экран",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = animatedOffset.x,
                    translationY = animatedOffset.y
                )
        )
    }
}
