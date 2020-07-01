package me.sedlar.calibreviewer

import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.redraw() {
    this.adapter = this.adapter // causes a redraw for whatever reason, ty android
}