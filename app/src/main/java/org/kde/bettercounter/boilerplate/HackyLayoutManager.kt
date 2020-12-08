package org.kde.bettercounter.boilerplate

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.util.Log


class HackyLayoutManager(context: Context) : LinearLayoutManager(context) {
    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            Log.e("Error", "Ignoring IndexOutOfBoundsException in RecyclerView")
        }
    }
}
