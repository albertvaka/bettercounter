package org.kde.bettercounter.ui

import android.view.HapticFeedbackConstants
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip.OnDismissListener
import org.kde.bettercounter.R
import org.kde.bettercounter.ViewModel
import org.kde.bettercounter.databinding.FragmentEntryBinding
import org.kde.bettercounter.databinding.FragmentEntryLongPressBinding
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Tutorial
import java.util.Calendar

class EntryViewHolder(
    private val activity: AppCompatActivity,
    val binding: FragmentEntryBinding,
    private var viewModel: ViewModel,
    private val touchHelper: ItemTouchHelper,
    private val onClickListener: (counter: CounterSummary) -> Unit?,
) : RecyclerView.ViewHolder(binding.root) {

    fun onBind(counter: CounterSummary) {
        binding.root.setBackgroundColor(counter.color.colorInt)
        binding.increaseButton.setOnClickListener {
            viewModel.incrementCounter(counter.name)
            if (!viewModel.isTutorialShown(Tutorial.PICK_DATE)) {
                viewModel.setTutorialShown(Tutorial.PICK_DATE)
                showPickDateTutorial()
            }
        }
        binding.increaseButton.setOnLongClickListener {
            showDateTimePicker(activity, Calendar.getInstance()) { pickedDateTime ->
                dialog(counter, true, pickedDateTime)
            }
            true
        }
        binding.decreaseButton.setOnClickListener { viewModel.decrementCounter(counter.name, true) }
        binding.decreaseButton.setOnLongClickListener {
            dialog(counter, false)
            true
        }
        binding.draggableArea.setOnClickListener { onClickListener(counter) }
        binding.draggableArea.setOnLongClickListener {
            touchHelper.startDrag(this@EntryViewHolder)
            @Suppress("DEPRECATION")
            binding.draggableArea.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING,
            )
            true
        }
        binding.nameText.text = counter.name
        binding.countText.text = counter.getFormattedCount()

        val checkDrawable = if (counter.isGoalMet()) R.drawable.ic_check else 0
        binding.countText.setCompoundDrawablesRelativeWithIntrinsicBounds(checkDrawable, 0, 0, 0)

        val mostRecentDate = counter.mostRecent
        if (mostRecentDate != null) {
            binding.timestampText.referenceTime = mostRecentDate.time
            binding.decreaseButton.isEnabled = true
        } else {
            binding.timestampText.referenceTime = -1L
            binding.decreaseButton.isEnabled = false
        }
    }

    fun showPickDateTutorial(onDismissListener: OnDismissListener? = null) {
        Tutorial.PICK_DATE.show(activity, binding.increaseButton, onDismissListener)
    }

    private fun dialog(counter: CounterSummary, mode: Boolean, pickedDateTime: Calendar = Calendar.getInstance()){
        val builder = AlertDialog.Builder(activity)
        val dialogBinding = FragmentEntryLongPressBinding.inflate(activity.layoutInflater)

        builder.setView(dialogBinding.root)
        dialogBinding.tietQuantity.requestFocus()

        builder.setPositiveButton(R.string.accept) { _, _ ->
            val tiet = dialogBinding.tietQuantity

            if (!tiet.text.isNullOrEmpty()){
                val quantity: Int = tiet.text.toString().toInt()
                if (quantity != 0){
                    if (mode) {
                        for (i in 0..<quantity) {
                            viewModel.incrementCounter(counter.name, pickedDateTime.time)
                        }
                    } else{
                        val builderConfirmation = AlertDialog.Builder(activity)

                        builderConfirmation.setTitle(R.string.titleDialogConfirmation)
                        builderConfirmation.setMessage(
                            activity.getString(R.string.confirmation_message, quantity)
                        )
                        builderConfirmation.setNegativeButton(R.string.cancel, null)

                        builderConfirmation.setCancelable(false)

                        builderConfirmation.setPositiveButton(R.string.yes) { _, _ ->
                            for (i in 0..<quantity){
                                viewModel.decrementCounter(counter.name, false)
                            }
                        }

                        val dialogConfirmation = builderConfirmation.create()

                        dialogConfirmation.show()
                    }

                }
            }else{
                if (mode) viewModel.incrementCounter(counter.name, pickedDateTime.time)
                else viewModel.decrementCounter(counter.name, true)
            }
        }

        builder.setNegativeButton(R.string.cancel) { _, _ ->
            // Toast.makeText(activity, "Canceled.", Toast.LENGTH_SHORT).show()
        }

        builder.create().show()
    }

}
