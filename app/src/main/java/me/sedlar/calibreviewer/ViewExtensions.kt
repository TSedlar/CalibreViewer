package me.sedlar.calibreviewer

import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.redraw(callback: (() -> Unit)? = null) {
    this.adapter = this.adapter // causes a redraw for whatever reason, ty android
    callback?.let { it() }
}