package org.kde.bettercounter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import org.kde.bettercounter.boilerplate.DragAndSwipeTouchHelper
import org.kde.bettercounter.databinding.FragmentEntryBinding
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Interval
import org.kde.bettercounter.ui.CounterSettingsDialogBuilder
import org.kde.bettercounter.ui.EntryViewHolder
import java.util.*

class EntryListViewAdapter(
    private var activity: AppCompatActivity,
    private var viewModel: ViewModel
) : RecyclerView.Adapter<EntryViewHolder>(), DragAndSwipeTouchHelper.ListGesturesCallback
{
    var onItemClickListener: ((Int, CounterSummary) -> Unit)? = null
    var onItemAdded: ((Int) -> Unit)? = null

    private val inflater: LayoutInflater = LayoutInflater.from(activity)
    private var counters: MutableList<String> = mutableListOf()

    override fun getItemCount(): Int = counters.size

    init {
        viewModel.observeNewCounter(activity, { counterName, isUserAdded ->
            counters.add(counterName)
            activity.runOnUiThread {
                val position = counters.size - 1
                notifyItemInserted(position)
                viewModel.getCounterSummary(counterName).observe(activity) {
                    notifyItemChanged(counters.indexOf(it.name), Unit) // passing a second parameter disables the disappear+appear animation
                }
                if (isUserAdded) {
                    onItemAdded?.invoke(position)
                }
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = FragmentEntryBinding.inflate(inflater, parent, false)
        val holder = EntryViewHolder(activity, binding, viewModel)
        binding.root.setOnClickListener {
            val counter = holder.counter
            if (counter != null) {
                onItemClickListener?.invoke(counters.indexOf(counter.name), counter)
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val counter = viewModel.getCounterSummary(counters[position]).value
        if (counter != null) {
            holder.onBind(counter)
        }
    }

    fun removeItem(position: Int) {
        val name = counters.removeAt(position)
        notifyItemRemoved(position)
        viewModel.deleteCounter(name)
        viewModel.saveCounterOrder(counters)
    }

    fun editCounter(position: Int, newName: String, interval : Interval) {
        val oldName = counters[position]
        if (oldName != newName) {
            counters[position] = newName
            viewModel.renameCounter(oldName, newName)
            viewModel.saveCounterOrder(counters)
        }
        viewModel.setCounterInterval(newName, interval)
        // The counter updates async, when the update is done we will get notified through the counter's livedata

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
        //TODO: haptic feedback?
    }

    override fun onDragEnd(viewHolder: RecyclerView.ViewHolder?) {
        viewModel.saveCounterOrder(counters)
    }

    override fun onSwipe(position: Int) {
        val name = counters[position]
        val interval = viewModel.getCounterInterval(name)
        CounterSettingsDialogBuilder(activity, viewModel)
            .forExistingCounter(name, interval)
            .setOnSaveListener { newName, newInterval ->
                editCounter(position, newName, newInterval)
            }
            .setOnDismissListener {
                notifyItemChanged(position) // moves the swiped item back to its place.
            }
            .setOnDeleteListener { _, _ ->
                removeItem(position)
            }
            .show()
    }

}
