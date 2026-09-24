package org.kde.bettercounter.ui.main

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.kde.bettercounter.R
import org.kde.bettercounter.persistence.CounterColors
import org.kde.bettercounter.persistence.CounterSummary

class EntryViewHolder(
    val binding: EntryBinding,
    private val onClickListener: (counter: CounterSummary) -> Unit,
    private val onIncrement: (counter: CounterSummary, tutorialAnchor: View) -> Unit,
    private val onDecrement: (counter: CounterSummary) -> Unit,
    private val onPickDate: (counter: CounterSummary) -> Unit,
    private val onDragRequest: (EntryViewHolder) -> Boolean
) : RecyclerView.ViewHolder(binding.root) {

    fun onBind(counter: CounterSummary) {
        binding.root.setBackgroundColor(counter.color.colorInt)
        val rippleRes = CounterColors.getInstance(binding.root.context).getRippleDrawableRes(counter.color)
        if (rippleRes != null) {
            binding.increaseButton.setBackgroundResource(rippleRes)
            binding.decreaseButton?.setBackgroundResource(rippleRes)
        } else {
            binding.increaseButton.background = null
            binding.decreaseButton?.background = null
        }
        binding.increaseButton.setOnClickListener {
            onIncrement(counter, binding.increaseButton)
        }
        binding.increaseButton.setOnLongClickListener {
            onPickDate(counter)
            true
        }
        binding.decreaseButton?.setOnClickListener { onDecrement(counter) }
        binding.draggableArea.setOnClickListener { onClickListener(counter) }
        binding.draggableArea.setOnLongClickListener { onDragRequest(this) }
        binding.nameText.text = counter.name
        binding.countText.text = counter.getFormattedCount()

        val checkDrawable = if (counter.isGoalMet()) R.drawable.ic_check else 0
        binding.countText.setCompoundDrawablesRelativeWithIntrinsicBounds(checkDrawable, 0, 0, 0)

        val mostRecentDate = counter.mostRecent
        if (mostRecentDate != null) {
            binding.timestampText.referenceTime = mostRecentDate.time
            binding.decreaseButton?.isEnabled = true
        } else {
            binding.timestampText.referenceTime = -1L
            binding.decreaseButton?.isEnabled = false
        }
    }
}
