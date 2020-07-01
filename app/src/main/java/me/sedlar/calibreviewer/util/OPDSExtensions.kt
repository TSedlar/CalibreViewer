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