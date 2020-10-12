package org.kde.bettercounter

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class EntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val contentView: TextView = view.findViewById(R.id.content)

    override fun toString(): String {
        return super.toString() + " '" + contentView.text + "'"
    }

    fun onBind(counter : String) {
        contentView.text = counter
    }
}