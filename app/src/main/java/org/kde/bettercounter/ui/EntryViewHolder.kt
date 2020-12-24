package org.kde.bettercounter.ui

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.kde.bettercounter.R
import org.kde.bettercounter.ViewModel
import org.kde.bettercounter.databinding.FragmentEntryBinding
import org.kde.bettercounter.persistence.CounterSummary


class EntryViewHolder(
    private val binding : FragmentEntryBinding,
    private var viewModel: ViewModel
) : RecyclerView.ViewHolder(binding.root) {

    var counter : CounterSummary? = null

    fun onBind(counter: CounterSummary) {
        this.counter = counter
        binding.increaseButton.setOnClickListener { viewModel.incrementCounter(counter.name) }
        binding.undoButton.setOnClickListener { viewModel.decrementCounter(counter.name) }
        binding.nameText.text = counter.name
        binding.countText.text = counter.count.toString()
        val lastEditDate = counter.lastEdit
        if (lastEditDate != null) {
            binding.timestampText.referenceTime = lastEditDate.time
            binding.undoButton.isEnabled = true
        } else {
            binding.timestampText.referenceTime = -1L
            binding.timestampText.setText(R.string.never)
            binding.undoButton.isEnabled = false
        }
    }
}
