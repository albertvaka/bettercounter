package org.kde.bettercounter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.kde.bettercounter.boilerplate.DragAndSwipeTouchHelper
import org.kde.bettercounter.persistence.Counter
import org.kde.bettercounter.persistence.Interval
import java.util.*
import kotlin.coroutines.CoroutineContext

class EntryListViewAdapter(
    private var activity: AppCompatActivity,
    private var viewModel: ViewModel,
    private var onItemClickListener: (pos : Int, counter : Counter) -> Unit
) : RecyclerView.Adapter<EntryViewHolder>(), DragAndSwipeTouchHelper.ListGesturesCallback,
    CoroutineScope {

    private val inflater: LayoutInflater = LayoutInflater.from(activity)
    private var counters: MutableList<String> = mutableListOf()

    override fun getItemCount(): Int = counters.size

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    init {
        viewModel.observeNewCounter(activity, { newCounter ->
            counters.add(newCounter)
            activity.runOnUiThread {
                notifyItemInserted(counters.size - 1)
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = inflater.inflate(R.layout.fragment_entry, parent, false)
        val holder = EntryViewHolder(view, viewModel)
        view.setOnClickListener {
            val counter = holder.counter
            if (counter != null) {
                onItemClickListener(counters.indexOf(counter.name), counter)
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val oldCounter = holder.counter
        if (oldCounter != null) {
            viewModel.getCounter(oldCounter.name)?.removeObservers(holder)
        }
        val counter = viewModel.getCounter(counters[position])?.value
        if (counter != null) {
            holder.onBind(counter)
        }
    }

    override fun onViewAttachedToWindow(holder: EntryViewHolder) {
        super.onViewAttachedToWindow(holder)
        val counter = holder.counter
        if (counter != null) {
            viewModel.getCounter(counter.name)?.observe(holder, {
                holder.onBind(it)
            })
        }
    }

    override fun onViewDetachedFromWindow(holder: EntryViewHolder) {
        val counter = holder.counter
        if (counter != null) {
            viewModel.getCounter(counter.name)?.removeObservers(holder)
        }
        super.onViewDetachedFromWindow(holder)
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
            if (newName.isBlank()) {
                Toast.makeText(activity, R.string.name_cant_be_blank, Toast.LENGTH_LONG).show()
                notifyItemChanged(position)
                return
            }
            if (viewModel.counterExists(newName)) {
                Toast.makeText(activity, R.string.already_exists, Toast.LENGTH_LONG).show()
                notifyItemChanged(position)
                return
            }
            counters[position] = newName
            viewModel.renameCounter(oldName, newName)
            viewModel.saveCounterOrder(counters)
        }
        viewModel.setCounterInterval(newName, interval)
        notifyItemChanged(position)
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
        val editView = inflater.inflate(R.layout.edit_counter, null)
        val textEdit = editView.findViewById<EditText>(R.id.text_edit)
        val spinner = editView.findViewById<Spinner>(R.id.interval_spinner)

        val name = counters[position]
        val interval = viewModel.getCounterInterval(name)

        textEdit.setText(name)
        val intervalAdapter = IntervalAdapter(activity)
        spinner.adapter = intervalAdapter
        spinner.setSelection(intervalAdapter.positionOf(interval))

        editView.findViewById<EditText>(R.id.text_edit).text.toString()
        AlertDialog.Builder(activity)
            .setTitle(R.string.edit_counter)
            .setView(editView)
            .setPositiveButton(R.string.save) { _, _ -> editCounter(
                position,
                textEdit.text.toString(),
                intervalAdapter.itemAt(spinner.selectedItemPosition)
            ) }
            .setNeutralButton(R.string.delete) { _, _ -> removeItem(position); }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .setOnCancelListener { notifyItemChanged(position) } // This cancels the swipe animation
            .show()
    }

}
