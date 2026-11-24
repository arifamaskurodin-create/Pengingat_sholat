package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IceBlueFixedDim
import com.example.ui.theme.NavyAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.tabularNums
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun TabularTimeWheelPicker(
    hour24: Int,
    minute: Int,
    is24Hour: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onAmPmChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onRoyalNavy: Boolean = false,
    itemHeight: Dp = 60.dp
) {
    val isPm = hour24 >= 12
    val displayHours = remember(is24Hour) {
        if (is24Hour) (0..23).toList() else (1..12).toList()
    }
    val currentDisplayHour = remember(hour24, is24Hour) {
        if (is24Hour) hour24 else {
            val h = hour24 % 12
            if (h == 0) 12 else h
        }
    }
    val minutes = remember { (0..59).toList() }

    val containerColor = if (onRoyalNavy) Color.Transparent else MaterialTheme.colorScheme.surface
    val selectionBandColor = if (onRoyalNavy) {
        Color.White.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val colonColor = if (onRoyalNavy) IceBlueFixedDim else MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(selectionBandColor)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            WheelColumn(
                items = displayHours,
                selectedItem = currentDisplayHour,
                onItemSelected = { selectedDisplayHour ->
                    val newHour24 = if (is24Hour) {
                        selectedDisplayHour
                    } else {
                        val base = if (selectedDisplayHour == 12) 0 else selectedDisplayHour
                        if (isPm) base + 12 else base
                    }
                    onHourChange(newHour24)
                },
                itemHeight = itemHeight,
                onRoyalNavy = onRoyalNavy,
                modifier = Modifier.width(84.dp)
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.displayLarge.tabularNums().copy(
                    fontSize = if (itemHeight < 56.dp) 34.sp else 40.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = colonColor,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            WheelColumn(
                items = minutes,
                selectedItem = minute,
                onItemSelected = onMinuteChange,
                itemHeight = itemHeight,
                onRoyalNavy = onRoyalNavy,
                modifier = Modifier.width(84.dp)
            )

            if (!is24Hour) {
                AmPmSelector(
                    isPm = isPm,
                    onSelect = onAmPmChange,
                    onRoyalNavy = onRoyalNavy,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    items: List<Int>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    itemHeight: Dp,
    onRoyalNavy: Boolean,
    modifier: Modifier = Modifier
) {
    val repeatMultiplier = 100
    val totalCount = items.size * repeatMultiplier
    val middleBlockStart = (repeatMultiplier / 2) * items.size
    val initialSelectedOffset = items.indexOf(selectedItem).coerceAtLeast(0)
    val initialIndex = middleBlockStart + initialSelectedOffset

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedItem) {
        val currentIdx = listState.firstVisibleItemIndex
        val currentVal = items[currentIdx % items.size]
        if (currentVal != selectedItem && !listState.isScrollInProgress) {
            val targetOffset = items.indexOf(selectedItem).coerceAtLeast(0)
            val blockStart = (currentIdx / items.size) * items.size
            val candidate = blockStart + targetOffset
            listState.animateScrollToItem(candidate)
        }
    }

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                kotlin.math.abs(itemCenter - viewportCenter)
            }?.index ?: listState.firstVisibleItemIndex
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { centeredIndex }
            .distinctUntilChanged()
            .filter { listState.isScrollInProgress }
            .collect { idx ->
                val value = items[idx % items.size]
                if (value != selectedItem) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onItemSelected(value)
                }
            }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                val value = items[centeredIndex % items.size]
                if (value != selectedItem) {
                    onItemSelected(value)
                }
            }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = itemHeight),
        modifier = modifier.height(itemHeight * 3),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(totalCount) { index ->
            val value = items[index % items.size]
            val isCentered = index == centeredIndex
            val activeColor = if (onRoyalNavy) Color.White else MaterialTheme.colorScheme.primary
            val inactiveColor = if (onRoyalNavy) IceBlueFixedDim else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            listState.animateScrollToItem(index)
                            onItemSelected(value)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format(Locale.US, "%02d", value),
                    style = MaterialTheme.typography.displayLarge.tabularNums().copy(
                        fontSize = when {
                            isCentered && itemHeight < 56.dp -> 34.sp
                            isCentered -> 40.sp
                            itemHeight < 56.dp -> 24.sp
                            else -> 28.sp
                        },
                        fontWeight = if (isCentered) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isCentered) activeColor else inactiveColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(if (isCentered) 1f else 0.48f)
                )
            }
        }
    }
}

@Composable
private fun AmPmSelector(
    isPm: Boolean,
    onSelect: (Boolean) -> Unit,
    onRoyalNavy: Boolean,
    modifier: Modifier = Modifier
) {
    val containerBg = if (onRoyalNavy) NavyAccent.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerBg)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(false to "AM", true to "PM").forEach { (pmValue, text) ->
            val selected = isPm == pmValue
            val pillBg = when {
                selected && onRoyalNavy -> Color.White
                selected -> MaterialTheme.colorScheme.primary
                else -> Color.Transparent
            }
            val textColor = when {
                selected && onRoyalNavy -> NavyPrimary
                selected -> MaterialTheme.colorScheme.onPrimary
                onRoyalNavy -> IceBlueFixedDim
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(pillBg)
                    .clickable { onSelect(pmValue) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor
                )
            }
        }
    }
}
