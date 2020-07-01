package me.sedlar.calibreviewer.util

import me.sedlar.calibre.opds.local.OPDSSeries
import java.io.Serializable

class SeriesFilter(val filter: (series: OPDSSeries) -> Boolean): Serializable {

    fun run(data: List<OPDSSeries>): List<OPDSSeries> {
        return data.filter(filter)
    }
}