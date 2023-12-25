package org.kde.bettercounter

import android.annotation.SuppressLint
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import org.kde.bettercounter.boilerplate.DragAndSwipeTouchHelper
import org.kde.bettercounter.databinding.FragmentEntryBinding
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Tutorial
import org.kde.bettercounter.ui.EntryViewHolder
import java.util.*

private const val TAG = "EntryListAdapter"

class EntryListViewAdapter(
    private var activity: AppCompatActivity,
    private var viewModel: ViewModel,
    private var listObserver: EntryListObserver,

) : RecyclerView.Adapter<EntryViewHolder>(), DragAndSwipeTouchHelper.ListGesturesCallback {
    interface EntryListObserver {
        fun onItemSelected(position: Int, counter: CounterSummary)
        fun onSelectedItemUpdated(position: Int, counter: CounterSummary)
        fun onItemAdded(position: Int)
    }

    var currentSelectedCounterName: String? = null
    fun clearItemSelected() { currentSelectedCounterName = null }

    private val inflater: LayoutInflater = LayoutInflater.from(activity)
    private var counters: MutableList<String> = mutableListOf()

    override fun getItemCount(): Int = counters.size

    private val touchHelper = ItemTouchHelper(DragAndSwipeTouchHelper(this))

    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(view: RecyclerView) {
        recyclerView = view
        touchHelper.attachToRecyclerView(view)
        super.onAttachedToRecyclerView(view)
    }

    init {
        viewModel.observeCounterChange(object : ViewModel.CounterObserver {

            fun observeNewCounter(counterName: String) {
                viewModel.getCounterSummary(counterName).observe(activity) {
                    notifyItemChanged(counters.indexOf(it.name), Unit)
                    if (currentSelectedCounterName == it.name) {
                        listObserver.onSelectedItemUpdated(counters.indexOf(it.name), it)
                    }
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onInitialCountersLoaded() {
                activity.runOnUiThread {
                    counters = viewModel.getCounterList().toMutableList()
                    notifyDataSetChanged()
                    for (counterName in counters) {
                        observeNewCounter(counterName)
                    }
                }
            }

            override fun onCounterAdded(counterName: String) {
                activity.runOnUiThread {
                    counters.add(counterName)
                    val position = counters.size - 1
                    notifyItemInserted(position)
                    observeNewCounter(counterName)
                    listObserver.onItemAdded(position)
                }
            }

            override fun onCounterRemoved(counterName: String) {
                val position = counters.indexOf(counterName)
                counters.removeAt(position)
                if (currentSelectedCounterName == counterName) {
                    currentSelectedCounterName = null
                }
                activity.runOnUiThread {
                    notifyItemRemoved(position)
                }
            }

            override fun onCounterRenamed(oldName: String, newName: String) {
                val position = counters.indexOf(oldName)
                counters[position] = newName
                if (currentSelectedCounterName == oldName) {
                    currentSelectedCounterName = newName
                    listObserver.onSelectedItemUpdated(position, viewModel.getCounterSummary(newName).value!!)
                }
                activity.runOnUiThread {
                    // passing a second parameter disables the disappear+appear animation
                    notifyItemChanged(position, Unit)
                }
            }

            override fun onCounterDecremented(counterName: String, oldEntryDate: Date) {
                Snackbar.make(
                    recyclerView!!,
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = FragmentEntryBinding.inflate(inflater, parent, false)
        val holder = EntryViewHolder(activity, binding, viewModel, touchHelper) { counter ->
            currentSelectedCounterName = counter.name
            listObserver.onItemSelected(counters.indexOf(counter.name), counter)
        }
        return holder
    }

    override fun onViewAttachedToWindow(holder: EntryViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (counters.size > 1 && !viewModel.isTutorialShown(Tutorial.DRAG)) {
            viewModel.setTutorialShown(Tutorial.DRAG)
            SimpleTooltip.Builder(activity)
                .anchorView(holder.binding.countText)
                .text(R.string.tutorial_drag)
                .gravity(Gravity.BOTTOM)
                .animated(true)
                .modal(true)
                .build()
                .show()
        }
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val counter = viewModel.getCounterSummary(counters[position]).value
        if (counter != null) {
            holder.onBind(counter)
        } else {
            Log.d(TAG, "Counter not found or still loading at pos $position")
        }
    }

    override fun onMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(counters, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(counters, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        // Do not store individual movements, store the final result in `onDragEnd`
    }

    override fun onDragStart(viewHolder: RecyclerView.ViewHolder?) {
        // nothing to do
    }

    override fun onDragEnd(viewHolder: RecyclerView.ViewHolder?) {
        viewModel.saveCounterOrder(counters)
    }
}
