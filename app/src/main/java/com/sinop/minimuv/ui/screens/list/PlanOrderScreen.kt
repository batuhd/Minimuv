package com.sinop.minimuv.ui.screens.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.sinop.minimuv.data.WatchStatus
import com.sinop.minimuv.ui.components.EmptyState
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.MidnightElevated
import com.sinop.minimuv.ui.theme.OutlineSoft
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.typeColor
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState

@Composable
fun PlanOrderScreen(onBack: () -> Unit) {
    val vm: ListViewModel = viewModel()
    val titles by vm.titles.collectAsStateWithLifecycle()

    val planItems = titles.orEmpty().filter { it.status == WatchStatus.PLAN.db }

    // Sürüklerken realtime güncellemeleri listeyi zıplatmasın diye sırayı LOKAL tutuyoruz.
    // Sunucuyla yalnızca: ilk yüklemede ya da öğe eklendi/çıktığında senkronlanır.
    var dragging by remember { mutableStateOf(false) }
    var orderIds by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(planItems.map { it.id }, dragging) {
        if (dragging) return@LaunchedEffect
        val serverIds = planItems
            .sortedWith(compareBy({ it.priorityOrder ?: Int.MAX_VALUE }, { it.title }))
            .map { it.id }
        val sameSet = serverIds.toSet() == orderIds.toSet()
        if (!sameSet || orderIds.isEmpty()) {
            orderIds = serverIds
        }
    }

    val byId = remember(titles) { titles.orEmpty().associateBy { it.id } }
    val ordered = orderIds.mapNotNull { byId[it] }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyColumnState(lazyListState) { from, to ->
        if (!dragging) {
            android.util.Log.w("MinimuvOrder", "move IGNORED (dragging=false) ${from.index}->${to.index}")
            return@rememberReorderableLazyColumnState
        }
        android.util.Log.d("MinimuvOrder", "move ${from.index} -> ${to.index}")
        orderIds = orderIds.toMutableList().apply {
            add(to.index.coerceIn(0, size), removeAt(from.index))
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
            }
            Column {
                Text("Sırada — sırayı düzenle", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Basılı tut & sürükle • bırakınca kaydedilir",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        if (ordered.isEmpty()) {
            EmptyState(
                emoji = "🗓️",
                title = "Sırada kimse yok",
                subtitle = "+ ile birkaç başlık ekle, sonra buradan izleme sırasına karar ver.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp,
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
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${index + 1}.",
                                style = MaterialTheme.typography.titleMedium,
                                color = typeColor(title.type),
                                modifier = Modifier.width(34.dp),
                            )
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
                                Text(
                                    "👥 İkimiz de istiyoruz",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                            Text("≡", style = MaterialTheme.typography.headlineSmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
