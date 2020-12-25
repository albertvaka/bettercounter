package org.kde.bettercounter.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import org.kde.bettercounter.R
import org.kde.bettercounter.ViewModel
import org.kde.bettercounter.databinding.FragmentEntryBinding
import org.kde.bettercounter.persistence.CounterSummary
import java.util.*


class EntryViewHolder(
    private val context : Context,
    private val binding : FragmentEntryBinding,
    private var viewModel: ViewModel
) : RecyclerView.ViewHolder(binding.root) {

    var counter : CounterSummary? = null

    fun onBind(counter: CounterSummary) {
        this.counter = counter
        binding.increaseButton.setOnClickListener { viewModel.incrementCounter(counter.name) }
        binding.increaseButton.setOnLongClickListener {
            showDateTimePicker(context, Calendar.getInstance()) { pickedDateTime ->
                viewModel.incrementCounter(counter.name, pickedDateTime.time)
            }
            true
        }
        binding.undoButton.setOnClickListener { viewModel.decrementCounter(counter.name) }
        binding.nameText.text = counter.name
        binding.countText.text = counter.count.toString()
        val mostRecentDate = counter.mostRecent
        if (mostRecentDate != null) {
            binding.timestampText.referenceTime = mostRecentDate.time
            binding.undoButton.isEnabled = true
        } else {
            binding.timestampText.referenceTime = -1L
            binding.timestampText.setText(R.string.never)
            binding.undoButton.isEnabled = false
        }
    }
}
