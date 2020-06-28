package me.sedlar.calibreviewer.holder

import me.sedlar.calibre.opds.local.OPDSLibrary
import me.sedlar.calibre.opds.local.OPDSSeries

data class SeriesHolder(val lib: OPDSLibrary, val series: OPDSSeries)