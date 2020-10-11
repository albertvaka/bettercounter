package org.kde.bettercounter

import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView


class ListTouchHelperCallback(private val mAdapter: ListTouchCallback) :
    ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean { return true }
    override fun isItemViewSwipeEnabled(): Boolean { return true }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.RIGHT
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
        val alertDialogBuilder = AlertDialog.Builder(viewHolder.itemView.context)
        alertDialogBuilder.setTitle("Delete?")
        alertDialogBuilder.setPositiveButton("Yeah") { _, _ ->
            mAdapter.removeItem(viewHolder.adapterPosition);
        }
        alertDialogBuilder.setNegativeButton("Nooo") { _, _ ->
            // Refresh so it re-appears
            mAdapter.refreshItem(viewHolder.adapterPosition);
        }
        alertDialogBuilder.show()
    }

    override fun onMove(
        recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        mAdapter.onMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSelectedChanged(
        viewHolder: RecyclerView.ViewHolder?,
        actionState: Int
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            mAdapter.onDragStart(viewHolder as EntryViewAdapter.ViewHolder)
        }
        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ) {
        super.clearView(recyclerView, viewHolder)
        mAdapter.onDragEnd(viewHolder as EntryViewAdapter.ViewHolder)
    }

    interface ListTouchCallback {
        fun removeItem(position: Int)
        fun refreshItem(position: Int)
        fun onMove(fromPosition: Int, toPosition: Int)
        fun onDragStart(myViewHolder: EntryViewAdapter.ViewHolder?)
        fun onDragEnd(myViewHolder: EntryViewAdapter.ViewHolder?)
    }
}