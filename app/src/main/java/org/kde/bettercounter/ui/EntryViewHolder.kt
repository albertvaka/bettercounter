package org.kde.bettercounter.ui

import android.content.Context
import android.view.Gravity
import android.view.HapticFeedbackConstants
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import org.kde.bettercounter.R
import org.kde.bettercounter.ViewModel
import org.kde.bettercounter.databinding.FragmentEntryBinding
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Tutorial
import java.util.*


class EntryViewHolder(
    private val context: Context,
    val binding: FragmentEntryBinding,
    private var viewModel: ViewModel,
    private val touchHelper: ItemTouchHelper,
    private val onClickListener: (counter : CounterSummary) -> Unit?,
) : RecyclerView.ViewHolder(binding.root) {

    fun onBind(counter: CounterSummary) {
        binding.root.setBackgroundColor(counter.color)
        binding.increaseButton.setOnClickListener {
            viewModel.incrementCounter(counter.name)
            if (!viewModel.isTutorialShown(Tutorial.PICKDATE)) {
                viewModel.setTutorialShown(Tutorial.PICKDATE)
                SimpleTooltip.Builder(context)
                    .anchorView(binding.increaseButton)
                    .text(R.string.tutorial_pickdate)
                    .gravity(Gravity.BOTTOM)
                    .animated(true)
                    .modal(true)
                    .build()
                    .show()
            }
        }
        binding.increaseButton.setOnLongClickListener {
            showDateTimePicker(context, Calendar.getInstance()) { pickedDateTime ->
                viewModel.incrementCounter(counter.name, pickedDateTime.time)
            }
            true
        }
        binding.decreaseButton.setOnClickListener { viewModel.decrementCounter(counter.name) }
        binding.draggableArea.setOnClickListener { onClickListener(counter) }
        binding.draggableArea.setOnLongClickListener {
            touchHelper.startDrag(this@EntryViewHolder)
            binding.draggableArea.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            true
        }
        binding.nameText.text = counter.name
        binding.countText.text = counter.lastIntervalCount.toString()
        val mostRecentDate = counter.mostRecent
        if (mostRecentDate != null) {
            binding.timestampText.referenceTime = mostRecentDate.time
            binding.decreaseButton.isEnabled = true
        } else {
            binding.timestampText.referenceTime = -1L
            binding.decreaseButton.isEnabled = false
        }
    }
}
