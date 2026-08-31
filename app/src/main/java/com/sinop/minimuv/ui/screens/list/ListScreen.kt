package com.sinop.minimuv.ui.screens.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sinop.minimuv.core.TextNormalizer
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.data.WatchStatus
import com.sinop.minimuv.ui.components.EmptyState
import com.sinop.minimuv.ui.components.PosterCard
import com.sinop.minimuv.ui.components.SectionTitle
import com.sinop.minimuv.ui.components.SoftChip
import com.sinop.minimuv.ui.components.StatusChip
import com.sinop.minimuv.ui.theme.Baloo2
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.MidnightElevated
import com.sinop.minimuv.ui.theme.OutlineSoft
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.statusColor
import com.sinop.minimuv.ui.theme.typeColor
import com.sinop.minimuv.ui.theme.typeEmoji
import java.util.Locale
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState

enum class SortOption(val label: String) {
    RECENT("Son Eklenen"),
    SCORE("Puan"),
    TITLE("İsim"),
    PRIORITY("Öncelik"),
}

/** Kütüphane görünümü — soldan sağa: en yoğundan en görsel ağırlıklıya. */
enum class ViewMode(val label: String) {
    COMPACT("Kompakt liste"),
    ROWS("Poster satırları"),
    GRID_3("3'lü ızgara"),
    GRID_2("2'li büyük ızgara");

    companion object {
        fun fromDb(value: String?): ViewMode = entries.firstOrNull { it.name == value } ?: GRID_3
    }
}

private fun ViewMode.icon(): ImageVector = when (this) {
    ViewMode.COMPACT -> Icons.Filled.ViewHeadline
    ViewMode.ROWS -> Icons.Filled.ViewAgenda
    ViewMode.GRID_3 -> Icons.Filled.GridView
    ViewMode.GRID_2 -> Icons.Filled.ViewModule
}

/** Eski "Rewatching" kayıtları artık "İzliyoruz" altında gösterilir. */
private fun statusGroup(db: String): String =
    if (db == WatchStatus.REWATCHING.db) WatchStatus.WATCHING.db else db

data class ListFilters(
    val type: ContentType? = null,
    val status: WatchStatus? = null,
    val customList: String? = null,
    val yearRange: ClosedFloatingPointRange<Float>? = null,
    val sort: SortOption = SortOption.RECENT,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListScreen(
    vm: ListViewModel,
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    onOpenTitle: (String) -> Unit,
    onOpenPlanOrder: () -> Unit,
    onEditTitle: (String) -> Unit = {},
) {
    val titles by vm.titles.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var filters by remember { mutableStateOf(ListFilters()) }
    var drawerOpen by remember { mutableStateOf(false) }
    var collapsedSections by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MidnightElevated,
                drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
            ) {
                FilterPanel(
                    filters = filters,
                    onChange = { filters = it },
                )
            }
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            // Üst bar: arama her zaman erişilebilir
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Kütüphanemizde ara… 🔍") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            Icon(
                                Icons.Filled.Close,
                                null,
                                Modifier.clickable { query = "" },
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MidnightCard,
                        unfocusedContainerColor = MidnightCard,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MidnightCard)
                        .clickable {
                            drawerOpen = true
                            scope.launch { drawerState.open() }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = "Filtreler",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Görünüm değiştirici: kompakt → satırlar → 3'lü ızgara → 2'li büyük
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ViewMode.entries.forEach { mode ->
                    val selected = viewMode == mode
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else MidnightCard
                            )
                            .clickable { onViewModeChange(mode) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            mode.icon(),
                            contentDescription = mode.label,
                            tint = if (selected) MaterialTheme.colorScheme.primary else TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    viewMode.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }

            // Tür sekmeleri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SoftChip(
                    label = "Hepsi",
                    selected = filters.type == null,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { filters = filters.copy(type = null) },
                )
                ContentType.entries.forEach { type ->
                    SoftChip(
                        label = type.label,
                        emoji = typeEmoji(type.db),
                        selected = filters.type == type,
                        color = typeColor(type.db),
                        onClick = {
                            filters = filters.copy(type = if (filters.type == type) null else type)
                        },
                    )
                }
            }

            when {
                loading && titles == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                titles == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Bir şeyler ters gitti 😵‍💫\n${vm.error.value.orEmpty()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
                else -> {
                    val filtered = filterAndSort(titles!!, query, filters, profiles)
                    if (filtered.isEmpty()) {
                        EmptyState(
                            emoji = "🍿",
                            title = if (query.isBlank() && filters == ListFilters()) {
                                "Henüz bir şey eklemediniz"
                            } else {
                                "Bu filtrelere uyan bir şey yok"
                            },
                            subtitle = if (query.isBlank() && filters == ListFilters()) {
                                "Sağ alttaki + ile ilk başlığınızı ekleyin, film geceniz başlasın!"
                            } else {
                                "Filtreleri gevşetin ya da + ile yeni bir şey ekleyin."
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (filters.status == null && filters.sort == SortOption.RECENT) {
                        // Ana görünüm: durum bazlı gruplanmış kütüphane
                        GroupedLibrary(
                            titles = filtered,
                            profiles = profiles,
                            viewMode = viewMode,
                            collapsedSections = collapsedSections,
                            onToggleSection = { key ->
                                collapsedSections = if (key in collapsedSections) collapsedSections - key
                                else collapsedSections + key
                            },
                            onOpenTitle = onOpenTitle,
                            onEditTitle = onEditTitle,
                            onOpenPlanOrder = onOpenPlanOrder,
                        )
                    } else if (filters.status == WatchStatus.PLAN && filters.sort == SortOption.PRIORITY) {
                        PlanPriorityList(
                            items = filtered,
                            vm = vm,
                            onOpenTitle = onOpenTitle,
                        )
                    } else {
                        FlatLibrary(filtered, profiles, viewMode, onOpenTitle, onEditTitle)
                    }
                }
            }
        }
    }
}

private fun filterAndSort(
    titles: List<Title>,
    query: String,
    filters: ListFilters,
    profiles: List<Profile>,
): List<Title> {
    val result = titles.filter { t ->
        val q = TextNormalizer.fold(query)
        val matchesQuery = q.isBlank() || TextNormalizer.fold(t.title).contains(q)
        val matchesType = filters.type == null || t.type == filters.type!!.db
        val matchesStatus = filters.status == null || t.status == filters.status!!.db
        val matchesCustomList = filters.customList == null || t.customLists.contains(filters.customList)
        val year = t.startDate?.take(4)?.toFloatOrNull()
        val matchesYear = filters.yearRange == null || (year != null && year >= filters.yearRange!!.start && year <= filters.yearRange!!.endInclusive)
        matchesQuery && matchesType && matchesStatus && matchesCustomList && matchesYear
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

@Composable
private fun GroupedLibrary(
    titles: List<Title>,
    profiles: List<Profile>,
    viewMode: ViewMode,
    collapsedSections: Set<String>,
    onToggleSection: (String) -> Unit,
    onOpenTitle: (String) -> Unit,
    onEditTitle: (String) -> Unit,
    onOpenPlanOrder: () -> Unit,
) {
    val sections = WatchStatus.entries.mapNotNull { s ->
        val items = titles.filter { statusGroup(it.status) == s.db }
        if (items.isEmpty()) null else s to items
    }
    when (viewMode) {
        ViewMode.GRID_3, ViewMode.GRID_2 -> {
            val columns = if (viewMode == ViewMode.GRID_3) 3 else 2
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                sections.forEach { (status, rawItems) ->
                    val collapsed = status.db in collapsedSections
                    if (collapsed) {
                        item(key = "header_${status.db}", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) {
                            SectionHeader(
                                status = status,
                                count = rawItems.size,
                                collapsed = true,
                                onToggle = { onToggleSection(status.db) },
                                showPlanOrder = false,
                                onOpenPlanOrder = onOpenPlanOrder,
                            )
                        }
                        return@forEach
                    }
                    // Sırada bölümü seçtiğimiz izleme sırasına göre dizilir
                    val sectionItems = if (status == WatchStatus.PLAN) {
                        rawItems.sortedWith(compareBy({ it.priorityOrder ?: Int.MAX_VALUE }, { it.title }))
                    } else {
                        rawItems
                    }
                    item(key = "header_${status.db}", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) {
                        SectionHeader(
                            status = status,
                            count = sectionItems.size,
                            collapsed = false,
                            onToggle = { onToggleSection(status.db) },
                            showPlanOrder = status == WatchStatus.PLAN && sectionItems.size > 1,
                            onOpenPlanOrder = onOpenPlanOrder,
                        )
                    }
                    items(items = sectionItems, key = { it.id }) { title ->
                        PosterCard(
                            title,
                            priorityLabel = if (status == WatchStatus.PLAN && title.priorityOrder != null) {
                                "${title.priorityOrder}. sırada"
                            } else null,
                            creatorEmoji = profiles.firstOrNull { it.id == title.createdByProfileId }?.emoji,
                            onClick = { onOpenTitle(title.id) },
                            onLongClick = { onEditTitle(title.id) },
                        )
                    }
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                sections.forEach { (status, rawItems) ->
                    val collapsed = status.db in collapsedSections
                    val sectionItems = if (status == WatchStatus.PLAN) {
                        rawItems.sortedWith(compareBy({ it.priorityOrder ?: Int.MAX_VALUE }, { it.title }))
                    } else {
                        rawItems
                    }
                    item(key = "header_${status.db}") {
                        SectionHeader(
                            status = status,
                            count = sectionItems.size,
                            collapsed = collapsed,
                            onToggle = { onToggleSection(status.db) },
                            showPlanOrder = status == WatchStatus.PLAN && sectionItems.size > 1,
                            onOpenPlanOrder = onOpenPlanOrder,
                        )
                    }
                    if (!collapsed) {
                        items(items = sectionItems, key = { it.id }) { title ->
                            TitleRow(
                                title = title,
                                viewMode = viewMode,
                                priorityLabel = if (status == WatchStatus.PLAN && title.priorityOrder != null) {
                                    "${title.priorityOrder}. sırada"
                                } else null,
                                onClick = { onOpenTitle(title.id) },
                                onLongClick = { onEditTitle(title.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlatLibrary(
    titles: List<Title>,
    profiles: List<Profile>,
    viewMode: ViewMode,
    onOpenTitle: (String) -> Unit,
    onEditTitle: (String) -> Unit = {},
) {
    when (viewMode) {
        ViewMode.GRID_3, ViewMode.GRID_2 -> {
            val columns = if (viewMode == ViewMode.GRID_3) 3 else 2
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(titles, key = { it.id }) { title ->
                    PosterCard(
                        title,
                        creatorEmoji = profiles.firstOrNull { it.id == title.createdByProfileId }?.emoji,
                        onClick = { onOpenTitle(title.id) },
                        onLongClick = { onEditTitle(title.id) },
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(items = titles, key = { it.id }) { title ->
                    TitleRow(
                        title = title,
                        viewMode = viewMode,
                        priorityLabel = null,
                        onClick = { onOpenTitle(title.id) },
                        onLongClick = { onEditTitle(title.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TitleRow(
    title: Title,
    viewMode: ViewMode,
    priorityLabel: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightCard)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (viewMode == ViewMode.ROWS) {
            Box(
                Modifier
                    .width(42.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MidnightElevated),
            ) {
                if (title.posterUrl != null) {
                    AsyncImage(
                        model = title.posterUrl,
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
                    .background(statusColor(title.status)),
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (viewMode == ViewMode.ROWS) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(title.status)
                    if (title.totalRewatches > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "🔁 ${title.totalRewatches}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                    priorityLabel?.let {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor(title.type),
                        )
                    }
                }
            }
        }
        title.score?.let { score ->
            Text(
                String.format(Locale.US, "★ %.1f", score),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFFD166),
                fontFamily = Baloo2,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            if (title.type != "film") {
                title.totalEpisodes?.let { "${title.episodeProgress}/$it" } ?: "${title.episodeProgress}"
            } else "",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(52.dp),
        )
        Text(
            ContentType.fromDb(title.type).label,
            style = MaterialTheme.typography.labelMedium,
            color = typeColor(title.type),
            textAlign = TextAlign.End,
            modifier = Modifier.width(48.dp),
        )
    }
}

@Composable
private fun SectionHeader(
    status: WatchStatus,
    count: Int,
    collapsed: Boolean,
    onToggle: () -> Unit,
    showPlanOrder: Boolean,
    onOpenPlanOrder: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 14.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(statusColor(status.db)),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            status.label,
            style = MaterialTheme.typography.titleMedium,
            color = statusColor(status.db),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "$count",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = if (collapsed) "Bölümü aç" else "Bölümü küçült",
            modifier = Modifier
                .size(18.dp)
                .rotate(if (collapsed) -90f else 0f),
            tint = TextSecondary,
        )
        if (showPlanOrder) {
            Spacer(Modifier.weight(1f))
            Text(
                "↕ Sırayı düzenle",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onOpenPlanOrder)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun PlanPriorityList(
    items: List<Title>,
    vm: ListViewModel,
    onOpenTitle: (String) -> Unit,
) {
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val idKey = items.joinToString(",") { it.id }
    var dragging by remember { mutableStateOf(false) }
    var orderIds by remember(idKey) { mutableStateOf(items.map { it.id }) }
    val byId = remember(items) { items.associateBy { it.id } }
    val ordered = orderIds.mapNotNull { byId[it] }
    val reorderableState = rememberReorderableLazyColumnState(lazyListState) { from, to ->
        if (!dragging) return@rememberReorderableLazyColumnState
        android.util.Log.d("MinimuvOrder", "move ${from.index} -> ${to.index}")
        orderIds = orderIds.toMutableList().apply {
            add(to.index.coerceIn(0, size), removeAt(from.index))
        }
    }
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(ordered, key = { _, t -> t.id }) { index, title ->
            ReorderableItem(reorderableState, key = title.id) { isDragging ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDragging) OutlineSoft else MidnightCard)
                        .longPressDraggableHandle(
                            onDragStarted = {
                                android.util.Log.d("MinimuvOrder", "drag started")
                                dragging = true
                            },
                            onDragStopped = {
                                android.util.Log.d("MinimuvOrder", "drag stopped, persisting $orderIds")
                                dragging = false
                                vm.reorderPlanList(orderIds)
                            },
                        )
                        .clickable { onOpenTitle(title.id) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${index + 1}.",
                        style = MaterialTheme.typography.titleMedium,
                        color = typeColor(title.type),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .width(44.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MidnightElevated),
                    ) {
                        if (title.posterUrl != null) {
                            AsyncImage(
                                model = title.posterUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            title.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val both = title.priorityOrder != null
                        Text(
                            if (both) "👥 İkimiz de istiyoruz" else "✨ Sırada",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (both) MaterialTheme.colorScheme.secondary else TextSecondary,
                        )
                    }
                    Text("≡", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun FilterPanel(
    filters: ListFilters,
    onChange: (ListFilters) -> Unit,
) {
    var yearRange by remember {
        mutableStateOf(filters.yearRange ?: 1960f..2026f)
    }
    Column(
        Modifier
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Filtreler", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            if (filters != ListFilters()) {
                Text(
                    "Sıfırla",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
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

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Durum", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SoftChip(
                    label = "Hepsi",
                    selected = filters.status == null,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onChange(filters.copy(status = null)) },
                )
                WatchStatus.entries.forEach { status ->
                    SoftChip(
                        label = status.label,
                        selected = filters.status == status,
                        color = statusColor(status.db),
                        onClick = {
                            onChange(filters.copy(status = if (filters.status == status) null else status))
                        },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Format", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SoftChip(
                    label = "Hepsi",
                    selected = filters.type == null,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onChange(filters.copy(type = null)) },
                )
                ContentType.entries.forEach { type ->
                    SoftChip(
                        label = type.label,
                        emoji = typeEmoji(type.db),
                        selected = filters.type == type,
                        color = typeColor(type.db),
                        onClick = {
                            onChange(filters.copy(type = if (filters.type == type) null else type))
                        },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Yıl", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                Spacer(Modifier.weight(1f))
                Text(
                    "${yearRange.start.toInt()} – ${yearRange.endInclusive.toInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            RangeSlider(
                value = yearRange,
                onValueChange = {
                    yearRange = it
                    onChange(filters.copy(yearRange = it))
                },
                valueRange = 1960f..2026f,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Sırala", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SortOption.entries.forEach { sort ->
                    SoftChip(
                        label = sort.label,
                        selected = filters.sort == sort,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { onChange(filters.copy(sort = sort)) },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
