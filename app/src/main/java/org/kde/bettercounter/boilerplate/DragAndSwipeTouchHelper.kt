package org.kde.bettercounter.boilerplate

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class DragAndSwipeTouchHelper(private val mAdapter: ListGesturesCallback) :
    ItemTouchHelper.Callback() {

    private var isDragging: Boolean = false

    override fun isLongPressDragEnabled(): Boolean = false // we start drag manually
    override fun isItemViewSwipeEnabled(): Boolean = true

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = 0
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        mAdapter.onMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (isDragging) {
            isDragging = false
            mAdapter.onDragEnd(viewHolder)
        }
        super.clearView(recyclerView, viewHolder)
    }

    override fun onSelectedChanged(
        viewHolder: RecyclerView.ViewHolder?,
        actionState: Int,
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            isDragging = true
            mAdapter.onDragStart(viewHolder)
        }
        super.onSelectedChanged(viewHolder, actionState)
    }

    interface ListGesturesCallback {
        fun onMove(fromPosition: Int, toPosition: Int)
        fun onDragStart(viewHolder: RecyclerView.ViewHolder?)
        fun onDragEnd(viewHolder: RecyclerView.ViewHolder?)
    }
}
