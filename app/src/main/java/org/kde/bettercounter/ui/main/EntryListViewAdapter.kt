package org.kde.bettercounter.ui.main

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip.OnDismissListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kde.bettercounter.R
import org.kde.bettercounter.boilerplate.DragAndSwipeTouchHelper
import org.kde.bettercounter.databinding.FragmentEntryBinding
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Tutorial
import java.util.Collections
import java.util.Date

class EntryListViewAdapter(
    private val activity: MainActivity,
    private val viewModel: MainActivityViewModel,
    private val listObserver: EntryListObserver,
) : RecyclerView.Adapter<EntryViewHolder>(), DragAndSwipeTouchHelper.ListGesturesCallback {

    val coroutineScope = CoroutineScope(Dispatchers.Main)

    interface EntryListObserver {
        fun onItemSelected(position: Int, counter: CounterSummary)
        fun onSelectedItemUpdated(position: Int, counter: CounterSummary)
        fun onItemAdded(position: Int)
        fun cancelFilter()
    }

    var currentSelectedCounterName: String? = null

    fun clearItemSelected() {
        currentSelectedCounterName = null
    }

    private val inflater: LayoutInflater = LayoutInflater.from(activity)
    private var counters: MutableList<String> = mutableListOf()
    private var filteredCounters: MutableList<String> = mutableListOf()
    private var filterQuery: String = ""

    override fun getItemCount(): Int = filteredCounters.size

    private val touchHelper = ItemTouchHelper(DragAndSwipeTouchHelper(this))

    override fun onAttachedToRecyclerView(view: RecyclerView) {
        touchHelper.attachToRecyclerView(view)
        super.onAttachedToRecyclerView(view)
    }

    init {
        viewModel.observeCounterChange(object : MainActivityViewModel.CounterObserver {

            fun observeNewCounter(counterName: String) {
                coroutineScope.launch {
                    viewModel.getCounterSummary(counterName).collect {
                        val position = filteredCounters.indexOf(it.name)
                        if (position >= 0) {
                            notifyItemChanged(position, Unit)
                            if (currentSelectedCounterName == it.name) {
                                listObserver.onSelectedItemUpdated(position, it)
                            }
                        }
                    }
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onInitialCountersLoaded() {
                activity.runOnUiThread {
                    counters = viewModel.getCounterList().toMutableList()
                    filteredCounters = counters.toMutableList()
                    notifyDataSetChanged()
                    for (counterName in counters) {
                        observeNewCounter(counterName)
                    }
                }
            }

            override fun onCounterAdded(counterName: String) {
                activity.runOnUiThread {
                    cancelFilter()
                    counters.add(counterName)
                    filteredCounters.add(counterName)
                    val position = counters.size - 1
                    notifyItemInserted(position)
                    observeNewCounter(counterName)
                    listObserver.onItemAdded(position)
                }
            }

            override fun onCounterRemoved(counterName: String) {
                cancelFilter()
                val position = counters.indexOf(counterName)
                counters.removeAt(position)
                filteredCounters.removeAt(position)
                if (currentSelectedCounterName == counterName) {
                    currentSelectedCounterName = null
                }
                activity.runOnUiThread {
                    notifyItemRemoved(position)
                }
            }

            override fun onCounterRenamed(oldName: String, newName: String) {
                cancelFilter()
                val position = counters.indexOf(oldName)
                counters[position] = newName
                filteredCounters[position] = newName
                activity.runOnUiThread {
                    // passing a second parameter disables the disappear+appear animation
                    notifyItemChanged(position, Unit)
                }
                if (currentSelectedCounterName == oldName) {
                    currentSelectedCounterName = newName
                    listObserver.onSelectedItemUpdated(position, viewModel.getCounterSummary(newName).value)
                }

            }

            override fun onCounterDecremented(counterName: String, oldEntryDate: Date) {
                Snackbar.make(
                    activity.binding.snackbar,
                    activity.getString(R.string.decreased_entry, counterName),
                    Snackbar.LENGTH_LONG
                )
                    .setAction(R.string.undo) {
                        viewModel.incrementCounter(counterName, oldEntryDate)
                    }
                    .show()
            }
        })
    }

    private fun cancelFilter() {
        if (filterQuery != "") {
            filterQuery = ""
            filteredCounters = counters.toMutableList()
            notifyDataSetChanged()
        }
        listObserver.cancelFilter()
    }

    fun applyFilter(query: String) {
        if (query == filterQuery) {
            return
        }
        filterQuery = query
        if (filterQuery.isBlank()) {
            filteredCounters = counters.toMutableList()
        } else {
            val lowerCaseQuery = query.trim().lowercase()
            filteredCounters = counters.asSequence().filter {
                it.lowercase().contains(lowerCaseQuery)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = FragmentEntryBinding.inflate(inflater, parent, false)
        val holder = EntryViewHolder(activity, binding, viewModel, touchHelper, ::selectCounter, ::canDrag)
        return holder
    }

    fun selectCounter(counter: CounterSummary) {
        currentSelectedCounterName = counter.name
        listObserver.onItemSelected(counters.indexOf(counter.name), counter)
    }

    override fun onViewAttachedToWindow(holder: EntryViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (counters.size > 1 && !viewModel.isTutorialShown(Tutorial.DRAG)) {
            viewModel.setTutorialShown(Tutorial.DRAG)
            showDragTutorial(holder)
        }
    }

    fun showDragTutorial(holder: EntryViewHolder, onDismissListener: OnDismissListener? = null) {
        SimpleTooltip.Builder(activity)
            .anchorView(holder.binding.countText)
            .text(R.string.tutorial_drag)
            .gravity(Gravity.BOTTOM)
            .animated(true)
            .focusable(true) // modal requires focusable
            .modal(true)
            .onDismissListener(onDismissListener)
            .build()
            .show()
    }

    fun showPickDateTutorial(holder: EntryViewHolder, onDismissListener: OnDismissListener? = null) {
        holder.showPickDateTutorial(onDismissListener)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val counterName = filteredCounters[position]
        val counter = viewModel.getCounterSummary(counterName).value
        holder.onBind(counter)
    }

    override fun onMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(counters, i, i + 1)
                Collections.swap(filteredCounters, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(counters, i, i - 1)
                Collections.swap(filteredCounters, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        // Do not store individual movements, store the final result in `onDragEnd`
    }

    override fun onDragStart(viewHolder: RecyclerView.ViewHolder?) {
        // nothing to do
    }

    override fun onDragEnd(viewHolder: RecyclerView.ViewHolder?) {
        assert(counters == filteredCounters) // do not save if the lists are out of sync
        viewModel.saveCounterOrder(counters)
    }

    fun canDrag(): Boolean {
        return filterQuery.isEmpty()
    }

}
