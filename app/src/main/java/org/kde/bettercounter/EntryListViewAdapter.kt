package org.kde.bettercounter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import org.kde.bettercounter.boilerplate.DragAndSwipeTouchHelper
import org.kde.bettercounter.databinding.FragmentEntryBinding
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Tutorial
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
        viewModel.observeCounterChange(activity, object : ViewModel.CounterAddedObserver {
            override fun onCounterAdded(counterName: String, isUserAdded: Boolean) {
                counters.add(counterName)
                activity.runOnUiThread {
                    val position = counters.size - 1
                    notifyItemInserted(position)
                    viewModel.getCounterSummary(counterName).observe(activity) {
                        notifyItemChanged(
                            counters.indexOf(it.name),
                            Unit
                        ) // passing a second parameter disables the disappear+appear animation
                    }
                    if (isUserAdded) {
                        onItemAdded?.invoke(position)
                    }
                }
            }

            override fun onCounterRemoved(counterName: String) {
                val position = counters.indexOf(counterName)
                counters.removeAt(position)
                activity.runOnUiThread {
                    notifyItemRemoved(position)
                }
            }

            override fun onCounterRenamed(oldName : String, newName: String) {
                val position = counters.indexOf(oldName)
                counters[position] = newName
                activity.runOnUiThread {
                    notifyItemChanged(position, Unit)
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
        //TODO: haptic feedback?
    }

    override fun onDragEnd(viewHolder: RecyclerView.ViewHolder?) {
        viewModel.saveCounterOrder(counters)
    }


}
