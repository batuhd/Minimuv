package com.sinop.minimuv.ui.screens.list

import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.WatchStatus

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

data class ListFilters(
    val type: ContentType? = null,
    val status: WatchStatus? = null,
    val yearRange: ClosedFloatingPointRange<Float>? = null,
    val sort: SortOption = SortOption.RECENT,
)
