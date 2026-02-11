package com.remindr.app.ui.screens.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.snapshotFlow
import com.kizitonwose.remindr.compose.HorizontalCalendar
import com.kizitonwose.remindr.compose.rememberCalendarState
import com.kizitonwose.remindr.core.*
import com.remindr.app.data.model.Item
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.ui.components.CalendarTitle
import com.remindr.app.ui.components.DayCell
import com.remindr.app.ui.components.ItemRow
import com.remindr.app.ui.components.MonthHeader
import com.remindr.app.ui.navigation.CalendarViewMode
import com.remindr.app.ui.theme.Colors
import kotlinx.coroutines.launch
import kotlinx.datetime.*

private val pageBackgroundColor: Color = Colors.example5PageBgColor
private val toolbarColor: Color = Colors.example5ToolbarColor
private val selectedItemColor: Color = Colors.accent

fun YearMonth.atStartOfMonth(): LocalDate = LocalDate(year, month, 1)
fun YearMonth.atEndOfMonth(): LocalDate {
    val start = atStartOfMonth()
    val nextMonth = start.plus(1, DateTimeUnit.MONTH)
    return nextMonth.minus(1, DateTimeUnit.DAY)
}

@Composable
fun CalendarScreen(
    items: List<Item>,
    groups: List<com.remindr.app.data.model.Group>,
    selection: CalendarDay?,
    onSelectionChange: (CalendarDay?) -> Unit,
    recentlyAddedDate: LocalDate?,
    onItemClick: (Item) -> Unit,
    onItemDelete: (Item) -> Unit,
    onItemStatusChange: (Long, ItemStatus) -> Unit,
    onSettingsClick: () -> Unit,
    viewMode: CalendarViewMode,
    onViewModeToggle: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(500) }
    val endMonth = remember { currentMonth.plusMonths(500) }
    val daysOfWeek = remember { daysOfWeek() }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first(),
        outDateStyle = OutDateStyle.EndOfGrid,
    )

    val visibleMonth = rememberFirstCompletelyVisibleMonth(state)
    LaunchedEffect(visibleMonth) {
        onSelectionChange(null)
    }

    val itemsInSelectedDate = remember(items, selection) {
        derivedStateOf {
            val date = selection?.date
            if (date == null) emptyList()
            else items.filter { it.time?.date == date }
        }
    }

    Scaffold(
        containerColor = pageBackgroundColor,
        topBar = {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                CalendarTitle(
                    modifier = Modifier
                        .background(toolbarColor)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    currentMonth = visibleMonth.yearMonth,
                    goToPrevious = {
                        coroutineScope.launch {
                            state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.previous)
                        }
                    },
                    goToNext = {
                        coroutineScope.launch {
                            state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.next)
                        }
                    },
                    onSettingsClick = onSettingsClick,
                    onTitleClick = onViewModeToggle,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 4.dp),
        ) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                AnimatedContent(
                    targetState = viewMode,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                ) { mode ->
                    when (mode) {
                        CalendarViewMode.Month -> {
                            HorizontalCalendar(
                                modifier = Modifier.wrapContentWidth(),
                                state = state,
                                dayContent = { day ->
                                    val itemsForDay = if (day.position == DayPosition.MonthDate) {
                                        items.filter { it.time?.date == day.date }
                                    } else {
                                        emptyList()
                                    }
                                    DayCell(
                                        day = day,
                                        isSelected = selection == day,
                                        isHighlighted = day.date == recentlyAddedDate,
                                        items = itemsForDay,
                                    ) { clicked ->
                                        onSelectionChange(clicked)
                                    }
                                },
                                monthHeader = {
                                    MonthHeader(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        daysOfWeek = daysOfWeek,
                                    )
                                },
                            )
                        }
                        CalendarViewMode.Year -> {
                            val initialPage = Int.MAX_VALUE / 2
                            val pagerState = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                            ) { page ->
                                val yearOffset = page - initialPage
                                val year = currentMonth.year + yearOffset

                                Column(modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = year.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                    )

                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        items(12) { index ->
                                            val monthName = Month.entries[index].name.take(3)
                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (year == currentMonth.year && index == currentMonth.month.ordinal)
                                                            selectedItemColor.copy(alpha = 0.5f)
                                                        else
                                                            Color.DarkGray,
                                                    )
                                                    .clickable {
                                                        coroutineScope.launch {
                                                            state.scrollToMonth(YearMonth(year, Month.entries[index]))
                                                            onViewModeToggle()
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(text = monthName, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = pageBackgroundColor)

                // Selected day items
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items = itemsInSelectedDate.value) { item ->
                            Box(
                                modifier = Modifier.clickable { onItemClick(item) },
                            ) {
                                ItemRow(
                                    item = item,
                                    onDelete = { onItemDelete(item) },
                                    onStatusChange = { status -> onItemStatusChange(item.id, status) },
                                    showGroupBadge = item.groupId != null,
                                    groupName = item.groupId?.let { gid -> groups.find { it.id == gid }?.name },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// Calendar utility composables
@Composable
private fun rememberFirstCompletelyVisibleMonth(state: com.kizitonwose.remindr.compose.CalendarState): CalendarMonth {
    val visibleMonth = remember(state) { mutableStateOf(state.firstVisibleMonth) }
    LaunchedEffect(state) {
        snapshotFlow { state.layoutInfo.completelyVisibleMonths.firstOrNull() }
            .collect { month -> if (month != null) visibleMonth.value = month }
    }
    return visibleMonth.value
}

private val com.kizitonwose.remindr.compose.CalendarLayoutInfo.completelyVisibleMonths: List<CalendarMonth>
    get() {
        val visibleItemsInfo = this.visibleMonthsInfo.toMutableList()
        return if (visibleItemsInfo.isEmpty()) {
            emptyList()
        } else {
            val lastItem = visibleItemsInfo.last()
            val viewportSize = this.viewportEndOffset + this.viewportStartOffset
            if (lastItem.offset + lastItem.size > viewportSize) {
                visibleItemsInfo.removeLast()
            }
            val firstItem = visibleItemsInfo.firstOrNull()
            if (firstItem != null && firstItem.offset < this.viewportStartOffset) {
                visibleItemsInfo.removeFirst()
            }
            visibleItemsInfo.map { it.month }
        }
    }

val YearMonth.next: YearMonth get() = this.plus(1, DateTimeUnit.MONTH)
val YearMonth.previous: YearMonth get() = this.minus(1, DateTimeUnit.MONTH)
