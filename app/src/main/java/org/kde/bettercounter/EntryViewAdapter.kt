package org.kde.bettercounter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class EntryViewAdapter(
    var types: MutableList<String>
) : RecyclerView.Adapter<EntryViewAdapter.ViewHolder>(), ListTouchHelperCallback.ListTouchCallback {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_entry, parent, false)

        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = types[position]
        holder.idView.text = position.toString()
        holder.contentView.text = item
    }

    override fun getItemCount(): Int = types.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val idView: TextView = view.findViewById(R.id.item_number)
        val contentView: TextView = view.findViewById(R.id.content)

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }

    override fun removeItem(position: Int) {
        types.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun refreshItem(position: Int) {
        notifyItemChanged(position)
    }

    override fun onMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(types, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(types, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onDragStart(myViewHolder: ViewHolder?) {
        //TODO: haptic feedback
    }

    override fun onDragEnd(myViewHolder: ViewHolder?) {
    }
}
