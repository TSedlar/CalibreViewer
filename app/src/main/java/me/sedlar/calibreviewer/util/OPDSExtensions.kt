package me.sedlar.calibreviewer.util

import me.sedlar.calibre.opds.local.OPDSSeries

fun OPDSSeries.matchesQuery(query: String): Boolean {
    val q = query.toLowerCase()

    if (name.toLowerCase().contains(q)) {
        return true
    } else if (tags.any { it.toLowerCase().contains(q) }) {
        return true
    }

    for (entry in this.entries) {
        if (entry.title.toLowerCase().contains(q)) {
            return true
        } else if (entry.authorName.toLowerCase().contains(q)) {
            return true
        }
    }

    return false
}

fun OPDSSeries.isComicOrManga(): Boolean {
    val entryMax = this.entries.size
    var comicCount = 0

    this.entries.forEach { entry ->
        for (acq in entry.acquisitions) {
            if (acq.fileExtension == "cbr" || acq.fileExtension == "cbz") {
                comicCount++
                break
            }
        }
    }

    // 75% or more entries are in comic format
    return (comicCount.toDouble() / entryMax.toDouble()) >= 0.75
}