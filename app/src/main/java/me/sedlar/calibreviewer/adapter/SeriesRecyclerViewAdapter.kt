package me.sedlar.calibreviewer.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import me.sedlar.calibre.opds.local.OPDSLibrary
import me.sedlar.calibre.opds.local.OPDSSeries
import me.sedlar.calibreviewer.EntryListActivity
import me.sedlar.calibreviewer.MainActivity
import me.sedlar.calibreviewer.R
import me.sedlar.calibreviewer.adapter.SeriesRecyclerViewAdapter.ItemViewHolder
import me.sedlar.calibreviewer.task.ImageDecodeTask
import me.sedlar.calibreviewer.util.SeriesFilter
import java.io.File


class SeriesRecyclerViewAdapter(
    private val activity: MainActivity,
    private val lib: OPDSLibrary,
    filter: SeriesFilter? = null
) : RecyclerView.Adapter<ItemViewHolder>() {

    private val data = filter?.run(lib.seriesList) ?: lib.seriesList

    init {
        println("SeriesRecyclerViewAdapter[size=${itemCount}]")
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): ItemViewHolder {
        val view: View = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_series, viewGroup, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ItemViewHolder, position: Int) {
        val series = data[position]
        val thumbnail = lib.getThumbFile(series, series.entries.first())
        val userDefinedThumbnail = File(thumbnail, "../cover.jpg")

        ImageDecodeTask(thumbnail, userDefinedThumbnail).execute(viewHolder.imgCover)

        viewHolder.lblSeriesTitle?.text = toTitleName(series.name)
        viewHolder.lblSeriesTitle?.visibility = if (activity.isShowingTitles()) View.VISIBLE else View.GONE

        viewHolder.lblSeriesCount?.text = series.entries.size.toString()

        viewHolder.itemView.setOnClickListener {
            val entryListIntent = Intent(it.context, EntryListActivity::class.java)
            entryListIntent.putExtra("lib", lib)
            entryListIntent.putExtra("series", series)

            it.context.startActivity(entryListIntent)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ItemViewHolder(itemView: View) : ViewHolder(itemView) {

        val imgCover: ImageView? = itemView.findViewById(R.id.imgCover)
        val lblSeriesTitle: TextView? = itemView.findViewById(R.id.lblSeriesTitle)
        val lblSeriesCount: TextView? = itemView.findViewById(R.id.lblSeriesCount)
    }

}

internal fun toTitleName(name: String): String {
    return name.replace(": ", ":\n")
}