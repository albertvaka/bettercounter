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
    var owner: AppCompatActivity,
    var viewModel: ViewModel
) : RecyclerView.Adapter<EntryViewHolder>(), DragAndSwipeTouchHelper.ListGesturesCallback,
    CoroutineScope {

    private val inflater: LayoutInflater = LayoutInflater.from(owner)
    private var counters: MutableList<String> = viewModel.counters.toMutableList()

    init {
        // Only observe additions, since it's the only change that happens externally.
        // Other changes are done by us and we don't want events when that happens,
        // since that gave problems (eg: drag animations ended abruptly since the list
        // content was refreshed as soon as the item was dropped).
        viewModel.observeAddCounter(owner, { counters ->
            counters?.let {
                this.counters = it.toMutableList()
                notifyDataSetChanged()
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = inflater.inflate(R.layout.fragment_entry, parent, false)
        return EntryViewHolder(view)
    }

    override fun getItemCount(): Int = counters.size

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.onBind(counters[position])
    }

    fun removeItem(position: Int) {
        counters.removeAt(position)
        notifyItemRemoved(position)
        viewModel.counters = counters
    }

    fun renameItem(position: Int, newName: String) {
        val oldName = counters[position]
        if (oldName == newName) {
            notifyItemChanged(position)
            return
        }
        if (counters.contains(newName)) {
            Toast.makeText(owner, R.string.already_exists, Toast.LENGTH_LONG).show()
            notifyItemChanged(position)
            return
        }
        counters[position] = newName
        viewModel.counters = counters
        viewModel.renameCounter(oldName, newName)
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
        viewModel.counters = counters
    }

    override fun onSwipe(position: Int) {
        val editView = inflater.inflate(R.layout.simple_edit_text, null)
        val textEdit = editView.findViewById<EditText>(R.id.text_edit)
        textEdit.setText(counters[position])
        editView.findViewById<EditText>(R.id.text_edit).text.toString()
        AlertDialog.Builder(owner)
            .setTitle("Edit counter")
            .setView(editView)
            .setPositiveButton("Save") { _, _ -> renameItem(position, textEdit.text.toString()) }
            .setNeutralButton("Delete") { _, _ -> removeItem(position); }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .setOnCancelListener() { notifyItemChanged(position) } // This cancels the swipe animation
            .show()
    }

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
}
