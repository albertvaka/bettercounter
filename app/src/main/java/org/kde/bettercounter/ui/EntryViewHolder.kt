package org.kde.bettercounter.ui

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.kde.bettercounter.R
import org.kde.bettercounter.ViewModel
import org.kde.bettercounter.persistence.CounterSummary


class EntryViewHolder(
    view: View,
    private var viewModel: ViewModel
) : RecyclerView.ViewHolder(view) {

    private val countText: TextView = view.findViewById(R.id.count)
    private val nameText: TextView = view.findViewById(R.id.name)
    private val timestampText : BetterRelativeTimeTextView = view.findViewById(R.id.timestamp)
    private val increaseButton: ImageButton = view.findViewById(R.id.btn_increase)
    private val undoButton: ImageButton = view.findViewById(R.id.btn_undo)

    var counter : CounterSummary? = null

    fun onBind(counter: CounterSummary) {
        this.counter = counter
        increaseButton.setOnClickListener { viewModel.incrementCounter(counter.name) }
        undoButton.setOnClickListener { viewModel.decrementCounter(counter.name) }
        nameText.text = counter.name
        countText.text = counter.count.toString()
        val lastEditDate = counter.lastEdit
        if (lastEditDate != null) {
            timestampText.referenceTime = lastEditDate.time
            undoButton.isEnabled = true
        } else {
            timestampText.referenceTime = -1L
            timestampText.setText(R.string.never)
            undoButton.isEnabled = false
        }
    }
}
