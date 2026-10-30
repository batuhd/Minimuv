package com.sinop.minimuv.ui.screens.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.data.WatchMode
import com.sinop.minimuv.data.WatchStatus
import com.sinop.minimuv.ui.components.CollapsibleHeader
import com.sinop.minimuv.ui.components.DcIconButton
import com.sinop.minimuv.ui.components.DcPaths
import com.sinop.minimuv.ui.components.FilterPill
import com.sinop.minimuv.ui.components.SvgPath
import com.sinop.minimuv.ui.components.SearchPill
import com.sinop.minimuv.ui.components.screenGlow
import com.sinop.minimuv.ui.theme.Fraunces
import com.sinop.minimuv.ui.theme.tokens
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyRowState

private fun statusGroup(db: String): String =
    if (db == WatchStatus.REWATCHING.db) WatchStatus.WATCHING.db else db

private fun libraryFilterAndSort(
    titles: List<Title>,
    filters: ListFilters,
): List<Title> {
    val result = titles.filter { t ->
        val matchesType = filters.type == null || t.type == filters.type!!.db
        val matchesStatus = filters.status == null || t.status == filters.status!!.db
        val year = t.startDate?.take(4)?.toFloatOrNull()
        val matchesYear = filters.yearRange == null ||
            (year != null && year >= filters.yearRange!!.start && year <= filters.yearRange!!.endInclusive)
        matchesType && matchesStatus && matchesYear
    }
    return when (filters.sort) {
        SortOption.RECENT -> result.sortedByDescending { it.createdAt ?: "" }
        SortOption.SCORE -> result.sortedWith(
            compareByDescending<Title> { it.score ?: -1.0 }.thenBy { it.title },
        )
        SortOption.TITLE -> result.sortedBy { it.title.lowercase() }
        SortOption.PRIORITY -> result.sortedWith(
            compareBy<Title> { it.priorityOrder ?: Int.MAX_VALUE }.thenBy { it.title },
        )
    }
}

/** Kütüphane ana ekranı — Main.dc.html düzeni. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    vm: ListViewModel,
    viewMode: ViewMode,
    displayLang: String?,
    onOpenTitle: (String) -> Unit,
    onEditTitle: (String) -> Unit = {},
    onViewModeChange: (ViewMode) -> Unit = {},
) {
    val titles by vm.titles.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val progressRows by vm.perProfileProgress.collectAsStateWithLifecycle()
    val tk = tokens()
    var filters by remember { mutableStateOf(ListFilters()) }
    var panelOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var pickerOpen by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(tk.bg)
            .screenGlow(vanAlpha = 0.28f, sinopAlpha = 0.30f),
    ) {
        if (titles == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = tk.van)
            }
        } else {
            val all = titles.orEmpty()
            val progressByTitle = progressRows.groupBy({ it.titleId }, { it.profileId to it.currentEpisode })
                .mapValues { entry -> entry.value.associate { it.first to it.second } }
            val typeDb = filters.type?.db
            val typeOk: (Title) -> Boolean = { typeDb == null || it.type == typeDb }
            val watching = all.filter { statusGroup(it.status) == WatchStatus.WATCHING.db && typeOk(it) }
                .sortedBy { it.priorityOrder ?: Int.MAX_VALUE }
            val specialFiltered = remember(all, filters) { libraryFilterAndSort(all, filters) }
            val watchingIds = watching.map { it.id }
            val planIds = all.filter { it.status == WatchStatus.PLAN.db }
                .sortedWith(compareBy({ it.priorityOrder ?: Int.MAX_VALUE }, { it.title }))
                .map { it.id }

            // Sıralamayı lokal tut: sürükleme sırasında realtime zıplamasın
            val gridState = rememberLazyGridState()
            val planOrder = remember { mutableStateListOf<String>() }
            var dragging by remember { mutableStateOf(false) }
            val continueRowState = rememberLazyListState()
            val continueOrder = remember { mutableStateListOf<String>() }
            var draggingContinue by remember { mutableStateOf(false) }
            val continueReorderState = rememberReorderableLazyRowState(continueRowState) { from, to ->
                if (!draggingContinue) return@rememberReorderableLazyRowState
                val fromId = from.key as? String ?: return@rememberReorderableLazyRowState
                val toId = to.key as? String ?: return@rememberReorderableLazyRowState
                val fromIdx = continueOrder.indexOf(fromId)
                val toIdx = continueOrder.indexOf(toId)
                if (fromIdx >= 0 && toIdx >= 0) {
                    continueOrder.add(toIdx, continueOrder.removeAt(fromIdx))
                }
            }
            LaunchedEffect(watchingIds, draggingContinue) {
                if (draggingContinue) return@LaunchedEffect
                if (watchingIds.toSet() != continueOrder.toSet() || continueOrder.isEmpty()) {
                    continueOrder.clear()
                    continueOrder.addAll(watchingIds)
                }
            }
            val collapsedSecs = remember { mutableStateListOf<String>() }
            val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
                if (!dragging) {
                    android.util.Log.w("MinimuvOrder", "move IGNORED (dragging=false) ${from.key} -> ${to.key}")
                    return@rememberReorderableLazyGridState
                }
                val fromId = from.key as? String ?: return@rememberReorderableLazyGridState
                val toId = to.key as? String ?: return@rememberReorderableLazyGridState
                val fromIdx = planOrder.indexOf(fromId)
                val toIdx = planOrder.indexOf(toId)
                if (fromIdx >= 0 && toIdx >= 0) {
                    android.util.Log.d("MinimuvOrder", "move $fromId -> idx $toIdx")
                    planOrder.add(toIdx, planOrder.removeAt(fromIdx))
                }
            }
            // Sunucu seti değişmedikçe (öğe eklenip çıkmadıkça) lokal sıraya dokunma
            LaunchedEffect(planIds, dragging) {
                if (dragging) return@LaunchedEffect
                if (planIds.toSet() != planOrder.toSet() || planOrder.isEmpty()) {
                    planOrder.clear()
                    planOrder.addAll(planIds)
                }
            }
            LaunchedEffect(viewMode) { gridState.scrollToItem(0) }
            val byId = remember(all) { all.associateBy { it.id } }
            val watchingOrdered = continueOrder.mapNotNull { byId[it] }.filter(typeOk)
            val planOrdered = planOrder.mapNotNull { byId[it] }.filter(typeOk)
            val q = query.trim()
            val searching = q.isNotEmpty()
            val searchResults = remember(all, filters, q) {
                libraryFilterAndSort(all, filters).filter { t ->
                    t.title.contains(q, ignoreCase = true) ||
                        (t.titleEn?.contains(q, ignoreCase = true) == true)
                }
            }

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(if (viewMode == ViewMode.GRID_2) 2 else 3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp,
                ),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LibraryHeader(
                        count = all.size,
                        profiles = profiles,
                        hasActiveFilters = filters != ListFilters(),
                        onSearch = { searchOpen = !searchOpen },
                        onFilterOpen = { panelOpen = true },
                        viewMode = viewMode,
                        onPickerOpen = { pickerOpen = true },
                    )
                }
                if (searchOpen) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SearchPill(
                            query = query,
                            placeholder = "Kütüphanede ara...",
                            onChange = { query = it },
                            onClose = { searchOpen = false; query = "" },
                            autoFocus = true,
                        )
                    }
                }

                if (searching) {
                    if (searchResults.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) { EmptyHint("Aramaya uyan bir şey yok") }
                    } else if (viewMode == ViewMode.COMPACT || viewMode == ViewMode.ROWS) {
                        items(
                            searchResults,
                            key = { it.id },
                            span = { GridItemSpan(maxLineSpan) },
                        ) { t ->
                            RowLibrary(
                                t, displayLang, viewMode == ViewMode.ROWS, onOpenTitle,
                                onEditTitle = { onEditTitle(t.id) },
                            )
                        }
                    } else {
                        items(searchResults, key = { it.id }) { t ->
                            PosterTile(
                                t,
                                displayLang,
                                badge = null,
                                meta = tileSub(t),
                                onClick = { onOpenTitle(t.id) },
                                onLongClick = { onEditTitle(t.id) },
                            )
                        }
                    }
                } else if (all.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { LibraryEmpty() }
                } else if (
                    filters.status != null ||
                    filters.sort != SortOption.RECENT || filters.yearRange != null
                ) {
                    val filtered = specialFiltered
                    if (filtered.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) { EmptyHint("Bu filtrelere uyan bir şey yok") }
                    } else if (viewMode == ViewMode.COMPACT || viewMode == ViewMode.ROWS) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                filtered.forEach { t ->
                                    RowLibrary(
                                        t, displayLang, viewMode == ViewMode.ROWS, onOpenTitle,
                                        onEditTitle = { onEditTitle(t.id) },
                                    )
                                }
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { t ->
                            PosterTile(
                                t,
                                displayLang,
                                badge = null,
                                meta = tileSub(t),
                                onClick = { onOpenTitle(t.id) },
                                onLongClick = { onEditTitle(t.id) },
                            )
                        }
                    }
                } else {
                    if (watching.isNotEmpty()) {
                        val secOpen = "Devam edelim" !in collapsedSecs
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CollapsibleHeader(
                                title = "Devam edelim",
                                trailing = "İzliyoruz · ${watching.size}",
                                collapsed = !secOpen,
                                modifier = Modifier.padding(top = 12.dp),
                                onToggle = {
                                    if (secOpen) collapsedSecs.add("Devam edelim") else collapsedSecs.remove("Devam edelim")
                                },
                            )
                        }
                        if (secOpen) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LazyRow(
                                    state = continueRowState,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(watchingOrdered, key = { it.id }) { t ->
                                        ReorderableItem(continueReorderState, key = t.id) { isDragging ->
                                            ContinueCard(
                                                t,
                                                displayLang,
                                                profiles,
                                                progressByTitle[t.id].orEmpty(),
                                                onClick = { onOpenTitle(t.id) },
                                                modifier = Modifier
                                                    .graphicsLayer {
                                                        if (isDragging) {
                                                            scaleX = 1.03f
                                                            scaleY = 1.03f
                                                        }
                                                    }
                                                    .longPressDraggableHandle(
                                                        onDragStarted = { draggingContinue = true },
                                                        onDragStopped = {
                                                            draggingContinue = false
                                                            vm.reorderPlanList(continueOrder.toList())
                                                        },
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (planOrdered.isNotEmpty()) {
                        val secOpen = "Sırada" !in collapsedSecs
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CollapsibleHeader(
                                title = "Sırada",
                                trailing = "${planOrdered.size} yapım",
                                collapsed = !secOpen,
                                modifier = Modifier.padding(top = 14.dp),
                                onToggle = {
                                    if (secOpen) collapsedSecs.add("Sırada") else collapsedSecs.remove("Sırada")
                                },
                            )
                        }
                        if (secOpen) {
                            if (viewMode == ViewMode.COMPACT || viewMode == ViewMode.ROWS) {
                                itemsIndexed(
                                    planOrdered,
                                    key = { _, t -> t.id },
                                    span = { _, _ -> GridItemSpan(maxLineSpan) },
                                ) { i, t ->
                                    ReorderableItem(reorderableState, key = t.id) { isDragging ->
                                        val rowInteraction = remember { MutableInteractionSource() }
                                        RowLibrary(
                                            t,
                                            displayLang,
                                            viewMode == ViewMode.ROWS,
                                            onOpenTitle,
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    if (isDragging) {
                                                        scaleX = 1.02f
                                                        scaleY = 1.02f
                                                        translationY = (-4).dp.toPx()
                                                        shadowElevation = 16.dp.toPx()
                                                        shape = RoundedCornerShape(14.dp)
                                                    }
                                                }
                                                .longPressDraggableHandle(
                                                    interactionSource = rowInteraction,
                                                    onDragStarted = { dragging = true },
                                                    onDragStopped = {
                                                        dragging = false
                                                        vm.reorderPlanList(planOrder.toList())
                                                    },
                                                ),
                                            interactionSource = rowInteraction,
                                            priorityLabel = "${i + 1}. sırada",
                                        )
                                    }
                                }
                            } else {
                                PlanGrid(
                                    plan = planOrdered,
                                    reorderableState = reorderableState,
                                    onDragStart = { dragging = true },
                                    onDragEnd = {
                                        dragging = false
                                        vm.reorderPlanList(planOrder.toList())
                                    },
                                    lang = displayLang,
                                    onClick = { t -> onOpenTitle(t.id) },
                                )
                            }
                        }
                    }

                    listOf(
                        "Tamamlandı" to WatchStatus.COMPLETED.db,
                        "Duraklattık" to WatchStatus.PAUSED.db,
                        "Bıraktık" to WatchStatus.DROPPED.db,
                    ).forEach { (label, db) ->
                        val secItems = all.filter { it.status == db && typeOk(it) }.sortedByDescending { it.createdAt.orEmpty() }
                        if (secItems.isEmpty()) return@forEach
                        val secOpen = label !in collapsedSecs
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CollapsibleHeader(
                                title = label,
                                trailing = "${secItems.size} yapım",
                                collapsed = !secOpen,
                                modifier = Modifier.padding(top = 16.dp),
                                onToggle = {
                                    if (secOpen) collapsedSecs.add(label) else collapsedSecs.remove(label)
                                },
                            )
                        }
                        if (secOpen) {
                            if (viewMode == ViewMode.COMPACT || viewMode == ViewMode.ROWS) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        secItems.forEach { t ->
                                            RowLibrary(
                                                t, displayLang, viewMode == ViewMode.ROWS, onOpenTitle,
                                                onEditTitle = { onEditTitle(t.id) },
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(secItems, key = { it.id }) { t ->
                                    PosterTile(
                                        t,
                                        displayLang,
                                        badge = null,
                                        meta = tileSub(t),
                                        onClick = { onOpenTitle(t.id) },
                                        onLongClick = { onEditTitle(t.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (panelOpen) {
            ModalBottomSheet(
                onDismissRequest = { panelOpen = false },
                containerColor = tk.surface1,
            ) {
                LibraryFilterPanel(
                    filters = filters,
                    onChange = { filters = it },
                )
            }
        }

        if (pickerOpen) {
            ModalBottomSheet(
                onDismissRequest = { pickerOpen = false },
                containerColor = tk.surface1,
            ) {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
                    Text(
                        "Görünüm",
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        color = tk.text,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    ViewModePickerCards(
                        current = viewMode,
                        onSelect = {
                            pickerOpen = false
                            onViewModeChange(it)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryChipRow(
    filters: ListFilters,
    onChange: (ListFilters) -> Unit,
    onPanelOpen: () -> Unit,
) {
    val tk = tokens()
    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterPill("Hepsi", filters.status == null, onClick = { onChange(filters.copy(status = null)) })
            FilterPill("İzliyoruz", filters.status == WatchStatus.WATCHING, onClick = {
                onChange(filters.copy(status = if (filters.status == WatchStatus.WATCHING) null else WatchStatus.WATCHING))
            })
            FilterPill("Sırada", filters.status == WatchStatus.PLAN, onClick = {
                onChange(filters.copy(status = if (filters.status == WatchStatus.PLAN) null else WatchStatus.PLAN))
            })
            FilterPill("Tamamlandı", filters.status == WatchStatus.COMPLETED, onClick = {
                onChange(filters.copy(status = if (filters.status == WatchStatus.COMPLETED) null else WatchStatus.COMPLETED))
            })
            DcIconButton(onClick = onPanelOpen, contentDescription = "Filtreler", size = 36.dp, background = tk.surface2) {
                SvgPath(DcPaths.FILTER, null, viewportSize = 16.dp)
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContentType.entries.forEach { type ->
                FilterPill(
                    type.label,
                    filters.type == type,
                    onClick = {
                        onChange(filters.copy(type = if (filters.type == type) null else type))
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryFilterPanel(
    filters: ListFilters,
    onChange: (ListFilters) -> Unit,
) {
    val tk = tokens()
    var yearRange by remember { mutableStateOf(filters.yearRange ?: 1960f..2026f) }
    Column(
        Modifier
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Filtreler",
                fontFamily = Fraunces,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = tk.text,
            )
            Spacer(Modifier.weight(1f))
            if (filters != ListFilters()) {
                Text(
                    "Sıfırla",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tk.van,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            yearRange = 1960f..2026f
                            onChange(ListFilters())
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        FilterGroup("Durum") {
            FilterPill("Hepsi", filters.status == null, onClick = { onChange(filters.copy(status = null)) })
            WatchStatus.entries.forEach { status ->
                FilterPill(
                    status.label,
                    filters.status == status,
                    onClick = {
                        onChange(filters.copy(status = if (filters.status == status) null else status))
                    },
                )
            }
        }

        FilterGroup("Format") {
            FilterPill("Hepsi", filters.type == null, onClick = { onChange(filters.copy(type = null)) })
            ContentType.entries.forEach { type ->
                FilterPill(
                    type.label,
                    filters.type == type,
                    onClick = {
                        onChange(filters.copy(type = if (filters.type == type) null else type))
                    },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Yıl", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = tk.textMuted)
                Spacer(Modifier.weight(1f))
                Text(
                    "${yearRange.start.toInt()} – ${yearRange.endInclusive.toInt()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tk.van,
                )
            }
            RangeSlider(
                value = yearRange,
                onValueChange = {
                    yearRange = it
                    onChange(filters.copy(yearRange = it))
                },
                valueRange = 1960f..2026f,
                colors = SliderDefaults.colors(
                    thumbColor = tk.text,
                    activeTrackColor = tk.van,
                    inactiveTrackColor = tk.surface3,
                ),
            )
        }

        FilterGroup("Sırala") {
            SortOption.entries.forEach { sort ->
                FilterPill(sort.label, filters.sort == sort, onClick = { onChange(filters.copy(sort = sort)) })
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FilterGroup(
    label: String,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val tk = tokens()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = tk.textMuted)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

@Composable
private fun LibraryHeader(
    count: Int,
    profiles: List<Profile>,
    hasActiveFilters: Boolean,
    onSearch: () -> Unit,
    onFilterOpen: () -> Unit,
    viewMode: ViewMode,
    onPickerOpen: () -> Unit,
) {
    val tk = tokens()
    Column(Modifier.statusBarsPadding()) {
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Kütüphane",
                    fontFamily = Fraunces,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 36.sp,
                    color = tk.text,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    companionNames(profiles) + " · $count yapım",
                    fontSize = 13.sp,
                    color = tk.textMuted,
                )
            }
            Spacer(Modifier.width(8.dp))
            DcIconButton(onClick = onSearch, contentDescription = "Kütüphanede ara") {
                SvgPath(DcPaths.SEARCH, null, viewportSize = 20.dp)
            }
            Spacer(Modifier.width(8.dp))
            Box {
                DcIconButton(onClick = onFilterOpen, contentDescription = "Filtreler") {
                    SvgPath(
                        DcPaths.FILTER,
                        null,
                        tint = if (hasActiveFilters) tk.van else tk.text,
                        viewportSize = 20.dp,
                    )
                }
                if (hasActiveFilters) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(tk.van),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            DcIconButton(onClick = onPickerOpen, contentDescription = "Görünümü değiştir") {
                SvgPath(viewIconPath(viewMode), null, viewportSize = 20.dp)
            }
        }
    }
}

private fun viewIconPath(m: ViewMode): String = when (m) {
    ViewMode.COMPACT -> DcPaths.VIEW_COMPACT
    ViewMode.ROWS -> DcPaths.VIEW_ROWS
    ViewMode.GRID_3 -> DcPaths.VIEW_GRID3
    ViewMode.GRID_2 -> DcPaths.VIEW_GRID2
}

@Composable
private fun ViewModePickerCards(
    current: ViewMode,
    onSelect: (ViewMode) -> Unit,
) {
    val tk = tokens()
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ViewMode.entries.forEach { m ->
            val selected = m == current
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tk.surface2)
                    .then(if (selected) Modifier.border(2.dp, tk.sinop, RoundedCornerShape(14.dp)) else Modifier)
                    .clickable { onSelect(m) }
                    .padding(horizontal = 4.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SvgPath(
                    viewIconPath(m),
                    null,
                    tint = if (selected) tk.text else tk.textMuted,
                    viewportSize = 20.dp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    m.label,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    color = if (selected) tk.text else tk.textMuted,
                    maxLines = 2,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

private fun companionNames(profiles: List<Profile>): String {
    if (profiles.isEmpty()) return "Minimuv"
    return profiles.joinToString(" & ") { p ->
        p.name.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(java.util.Locale.getDefault()) else c.toString() }
    }
}

private fun isAyri(watchMode: String?): Boolean {
    if (watchMode.isNullOrBlank()) return false
    val mode = watchMode.trim().lowercase()
    return mode == "ayri" || mode == "ayrı"
}
/* ---------- Devam kartı (300×168) ---------- */

@Composable
private fun ContinueCard(
    t: Title,
    lang: String?,
    profiles: List<Profile>,
    progress: Map<String, Int>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tk = tokens()
    Box(
        modifier
            .width(300.dp)
            .height(168.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(tk.surface2)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .width(108.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .background(tk.surface3),
            ) {
                if (t.posterUrl != null) {
                    AsyncImage(
                        model = t.posterUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
                Column {
                    Text(
                        ContentType.fromDb(t.type).label,
                        fontSize = 11.sp,
                        color = tk.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        t.displayTitle(lang),
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 19.sp,
                        color = tk.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.weight(1f))
                val totalStr = t.totalEpisodes?.let { " / $it" } ?: ""
                if (isAyri(t.watchMode) && profiles.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        profiles.forEachIndexed { i, p ->
                            val ep = progress[p.id] ?: 0
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AvatarDot(
                                    p,
                                    if (i == 0) tk.van else tk.sinop,
                                    if (i == 0) tk.onVan else tk.onSinop,
                                )
                                Spacer(Modifier.width(7.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Bölüm $ep$totalStr",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = tk.textSoft,
                                        modifier = Modifier.padding(bottom = 4.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    ContinueProgressBar(epFraction(ep, t.totalEpisodes))
                                }
                            }
                        }
                    }
                } else {
                    Column {
                        val currentEp = if (isAyri(t.watchMode) && progress.isNotEmpty()) {
                            progress.values.maxOrNull() ?: t.episodeProgress
                        } else {
                            t.episodeProgress
                        }
                        Text(
                            "Bölüm $currentEp$totalStr",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = tk.textSoft,
                            modifier = Modifier.padding(bottom = 5.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        ContinueProgressBar(epFraction(currentEp, t.totalEpisodes))
                        Row(
                            Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.width(32.dp), contentAlignment = Alignment.CenterStart) {
                                AvatarDot(profiles.getOrNull(0), tk.van, tk.onVan)
                                AvatarDot(
                                    profiles.getOrNull(1),
                                    tk.sinop,
                                    tk.onSinop,
                                    modifier = Modifier.offset(x = 12.dp),
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                continueNote(t, profiles, progress),
                                fontSize = 11.sp,
                                color = tk.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun epFraction(ep: Int, total: Int?): Double =
    total?.takeIf { it > 0 }?.let { (ep.toDouble() / it).coerceIn(0.0, 1.0) } ?: 0.0

@Composable
private fun ContinueProgressBar(fraction: Double) {
    val tk = tokens()
    Box(
        Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(2.5.dp))
            .background(tk.surface3),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.toFloat())
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(listOf(tk.van, tk.sinop)),
                    RoundedCornerShape(2.5.dp),
                ),
        )
    }
}

private fun continueNote(
    t: Title,
    profiles: List<Profile>,
    progress: Map<String, Int>,
): String {
    if (!isAyri(t.watchMode)) return "Birlikte izliyoruz"
    if (profiles.isEmpty()) return "Ayrı izliyoruz"
    return profiles.joinToString(" · ") { p ->
        val name = p.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        val ep = progress[p.id] ?: 0
        "$name: $ep"
    }
}

@Composable
private fun AvatarDot(profile: Profile?, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    val tk = tokens()
    val name = profile?.name?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    Box(
        modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (profile?.avatarUrl == null) bg else Color.Transparent, CircleShape)
            .border(2.dp, tk.surface2, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (profile?.avatarUrl != null) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                name?.firstOrNull()?.uppercase()?.toString().orEmpty(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1,
            )
        }
    }
}

/* ---------- Poster karesi + Sırada izgarası ---------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PosterTile(
    t: Title,
    lang: String?,
    badge: String?,
    meta: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    isDragging: Boolean = false,
) {
    val tk = tokens()
    Column(
        modifier = modifier.graphicsLayer {
            if (isDragging) {
                scaleX = 1.05f
                scaleY = 1.05f
                translationY = -8.dp.toPx()
                shadowElevation = 24.dp.toPx()
            }
        }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .background(tk.surface3)
                .combinedClickable(
                    interactionSource = interactionSource,
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        ) {
            if (t.posterUrl != null) {
                AsyncImage(
                    model = t.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            badge?.let {
                Box(
                    Modifier
                        .padding(start = 8.dp, top = 8.dp)
                        .size(24.dp)
                        .background(Color(0xFF0E0E12).copy(alpha = 0.75f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(it, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)),
                        ),
                    )
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 28.dp),
            ) {
                Text(
                    t.displayTitle(lang),
                    fontFamily = Fraunces,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.85f),
                            offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                            blurRadius = 6f,
                        ),
                    ),
                )
            }
        }
        if (meta != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                meta,
                fontSize = 11.sp,
                color = tk.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

private fun tileSub(t: Title): String {
    val label = ContentType.fromDb(t.type).label
    val year = t.startDate?.take(4)
    return when (t.type) {
        "film" -> "$label · $year"
        else -> {
            val total = t.totalEpisodes
            if (total != null && total > 0) "$label · $total bölüm" else "$label · $year"
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyGridScope.PlanGrid(
    plan: List<Title>,
    reorderableState: ReorderableLazyGridState,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    lang: String?,
    onClick: (Title) -> Unit,
) {
    itemsIndexed(plan, key = { _, t -> t.id }) { i, t ->
        ReorderableItem(reorderableState, key = t.id) { isDragging ->
            val dragInteraction = remember { MutableInteractionSource() }
            PosterTile(
                t,
                lang,
                "${i + 1}",
                meta = tileSub(t),
                onClick = { onClick(t) },
                modifier = Modifier.longPressDraggableHandle(
                    interactionSource = dragInteraction,
                    onDragStarted = {
                        android.util.Log.d("MinimuvOrder", "drag started idx=$i (${t.title})")
                        onDragStart()
                    },
                    onDragStopped = {
                        android.util.Log.d("MinimuvOrder", "drag stopped idx=$i, persisting")
                        onDragEnd()
                    },
                ),
                interactionSource = dragInteraction,
                isDragging = isDragging,
            )
        }
    }
}

/* ---------- Kompakt liste / poster satırları ---------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowLibrary(
    t: Title,
    lang: String?,
    withPoster: Boolean,
    onOpenTitle: (String) -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    onEditTitle: (() -> Unit)? = null,
    priorityLabel: String? = null,
) {
    val tk = tokens()
    val clickSource = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tk.surface2)
            .combinedClickable(
                interactionSource = interactionSource ?: clickSource,
                role = Role.Button,
                onClick = { onOpenTitle(t.id) },
                onLongClick = onEditTitle,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (withPoster) {
            Box(
                Modifier
                    .width(42.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tk.surface3),
            ) {
                if (t.posterUrl != null) {
                    AsyncImage(
                        model = t.posterUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
        } else {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(com.sinop.minimuv.ui.theme.statusColor(t.status)),
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                t.displayTitle(lang),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = tk.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (withPoster) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        WatchStatus.fromDb(t.status).label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tk.textMuted,
                    )
                    if (priorityLabel != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(priorityLabel, fontSize = 11.sp, color = tk.textMuted)
                    }
                    if (t.totalRewatches > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text("Yeniden ×${t.totalRewatches}", fontSize = 11.sp, color = tk.textMuted)
                    }
                }
            }
        }
        t.score?.let { score ->
            Text(
                String.format(java.util.Locale.US, "%.1f", score),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = tk.gold,
            )
            Spacer(Modifier.width(10.dp))
        }
        if (t.type != "film") {
            Text(
                t.totalEpisodes?.let { "${t.episodeProgress}/$it" } ?: "${t.episodeProgress}",
                fontSize = 12.sp,
                color = tk.textMuted,
                modifier = Modifier.width(52.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            ContentType.fromDb(t.type).label,
            fontSize = 12.sp,
            color = tk.textMuted,
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    val tk = tokens()
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 14.sp, color = tk.textMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/* ---------- Boş kütüphane ---------- */

@Composable
private fun LibraryEmpty() {
    val tk = tokens()
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Henüz bir şey eklemediniz",
                fontFamily = Fraunces,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = tk.text,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Ara sekmesinden ilk başlığınızı ekleyin, film geceniz başlasın.",
                fontSize = 13.sp,
                color = tk.textMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/* ---------- Önizlemeler ---------- */

private val previewProfiles = listOf(
    Profile(id = "1", name = "Van"),
    Profile(id = "2", name = "Sinop"),
)

@androidx.compose.ui.tooling.preview.Preview(
    name = "Kütüphane Başlık",
    showBackground = true,
    backgroundColor = 0xFF0E0E12,
    widthDp = 390,
    heightDp = 160,
)
@Composable
private fun LibraryHeaderPreview() {
    com.sinop.minimuv.ui.theme.MinimuvTheme {
        LibraryHeader(
            count = 48,
            profiles = previewProfiles,
            hasActiveFilters = false,
            onSearch = {},
            onFilterOpen = {},
            viewMode = ViewMode.GRID_3,
            onPickerOpen = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Devam Kartı",
    showBackground = true,
    backgroundColor = 0xFF0E0E12,
    widthDp = 390,
    heightDp = 240,
)
@Composable
private fun ContinueCardPreview() {
    com.sinop.minimuv.ui.theme.MinimuvTheme {
        ContinueCard(
            Title(id = "1", type = "dizi", title = "Sessiz Liman", episodeProgress = 7, totalEpisodes = 12),
            null,
            previewProfiles,
            mapOf("1" to 9, "2" to 7),
            onClick = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Sırada Karesi",
    showBackground = true,
    backgroundColor = 0xFF0E0E12,
    widthDp = 160,
    heightDp = 280,
)
@Composable
private fun PosterTilePreview() {
    com.sinop.minimuv.ui.theme.MinimuvTheme {
        Column(Modifier.background(tokens().bg).padding(12.dp)) {
            PosterTile(
                Title(id = "2", type = "film", title = "Cam Şehir", startDate = "2024-03-01"),
                null,
                "1",
                meta = tileSub(Title(id = "2", type = "film", title = "Cam Şehir", startDate = "2024-03-01")),
                onClick = {},
            )
        }
    }
}
