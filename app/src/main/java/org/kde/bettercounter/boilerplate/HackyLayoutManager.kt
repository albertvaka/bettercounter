package org.kde.bettercounter.boilerplate

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class HackyLayoutManager(context: Context, @RecyclerView.Orientation orientation:Int = RecyclerView.VERTICAL)
    : LinearLayoutManager(context, orientation, false) {
    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            Log.e("Error", "Ignoring IndexOutOfBoundsException in RecyclerView")
        }
    }
}
