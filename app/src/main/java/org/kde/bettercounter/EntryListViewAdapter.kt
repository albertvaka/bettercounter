package org.kde.bettercounter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.kde.bettercounter.boilerplate.DragAndSwipeTouchHelper
import java.util.*
import kotlin.coroutines.CoroutineContext

class EntryListViewAdapter(
    private var owner: AppCompatActivity,
    private var viewModel: ViewModel
) : RecyclerView.Adapter<EntryViewHolder>(), DragAndSwipeTouchHelper.ListGesturesCallback,
    CoroutineScope {

    private val inflater: LayoutInflater = LayoutInflater.from(owner)
    private var counters: MutableList<String> = mutableListOf()

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    init {
        viewModel.observeNewCounter(owner, { newCounter ->
            counters.add(newCounter)
            notifyItemInserted(counters.size-1)
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = inflater.inflate(R.layout.fragment_entry, parent, false)
        return EntryViewHolder(view, viewModel)
    }

    override fun getItemCount(): Int = counters.size

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        viewModel.getCounter(counters[position]).removeObservers(owner)
        viewModel.getCounter(counters[position]).observe(owner, {
            holder.onBind(it)
        })
    }

    fun removeItem(position: Int) {
        val name = counters.removeAt(position)
        notifyItemRemoved(position)
        viewModel.deleteCounter(name)
        viewModel.saveCounterOrder(counters)
    }

    fun renameItem(position: Int, newName: String) {
        val oldName = counters[position]
        if (oldName == newName) {
            notifyItemChanged(position)
            return
        }
        if (viewModel.counterExists(newName)) {
            Toast.makeText(owner, R.string.already_exists, Toast.LENGTH_LONG).show()
            notifyItemChanged(position)
            return
        }
        counters[position] = newName
        viewModel.renameCounter(oldName, newName)
        viewModel.saveCounterOrder(counters)
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
        //TODO: haptic feedback
    }

    override fun onDragEnd(viewHolder: RecyclerView.ViewHolder?) {
        viewModel.saveCounterOrder(counters)
    }

    override fun onSwipe(position: Int) {
        val editView = inflater.inflate(R.layout.simple_edit_text, null)
        val textEdit = editView.findViewById<EditText>(R.id.text_edit)
        textEdit.setText(counters[position])
        editView.findViewById<EditText>(R.id.text_edit).text.toString()
        AlertDialog.Builder(owner)
            .setTitle(R.string.edit_counter)
            .setView(editView)
            .setPositiveButton(R.string.save) { _, _ -> renameItem(position, textEdit.text.toString()) }
            .setNeutralButton(R.string.delete) { _, _ -> removeItem(position); }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .setOnCancelListener { notifyItemChanged(position) } // This cancels the swipe animation
            .show()
    }

}
