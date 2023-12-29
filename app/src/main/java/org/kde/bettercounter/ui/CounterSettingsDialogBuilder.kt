package org.kde.bettercounter.ui

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputLayout
import org.kde.bettercounter.ColorAdapter
import org.kde.bettercounter.IntervalAdapter
import org.kde.bettercounter.R
import org.kde.bettercounter.ViewModel
import org.kde.bettercounter.databinding.EditCounterBinding
import org.kde.bettercounter.persistence.CounterColor
import org.kde.bettercounter.persistence.CounterMetadata
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Interval

class CounterSettingsDialogBuilder(private val context: Context, private val viewModel: ViewModel) {

    private val builder: AlertDialog.Builder = AlertDialog.Builder(context)
    private val binding: EditCounterBinding = EditCounterBinding.inflate(LayoutInflater.from(context))
    private val intervalAdapter = IntervalAdapter(context)
    private val colorAdapter = ColorAdapter(context)
    private var onSaveListener: (counterMetadata: CounterMetadata) -> Unit = { _ -> }
    private var previousName: String? = null
    private var goal = 0

    init {
        builder.setView(binding.root)

        binding.spinnerInterval.adapter = intervalAdapter

        // The material equivalent to a spinner is an AutoCompleteTextEdit with the ExposedDropdown
        // style and some settings tweaked to always show all the "autocomplete" options once clicked.
        // However, despite both the Spinner and the AutoCompleteTextEdit using a ListPopupWindow to
        // display the dropdown with the list of options, the Spinner sets the layout type to
        // TYPE_APPLICATION_SUB_PANEL and the AutoCompleteTextEdit doesn't. We want this setting
        // because it makes the dropdown be displayed above the on-screen keyboard. This prevents the
        // dialog moving up and down and lets the user enter the name for the counter before or after
        // the other options. So we are displaying the selected option in a TextView for the material
        // looks but using a hidden Spinner for the dropdown behavior.
        binding.fakeSpinnerIntervalBox.setOnClickListener {
            binding.spinnerInterval.performClick()
        }
        binding.fakeSpinnerInterval.setOnClickListener {
            binding.spinnerInterval.performClick()
        }
        binding.fakeSpinnerIntervalBox.endIconMode = TextInputLayout.END_ICON_CUSTOM
        binding.fakeSpinnerIntervalBox.endIconDrawable = AppCompatResources.getDrawable(context, com.google.android.material.R.drawable.mtrl_dropdown_arrow)
        binding.fakeSpinnerInterval.isLongClickable = false
        binding.spinnerInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long
            ) {
                binding.fakeSpinnerInterval.setText(intervalAdapter.getItem(position))
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }

        binding.colorpicker.adapter = colorAdapter
        binding.colorpicker.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        binding.goalInputBox.setStartIconOnClickListener {
            if (goal > 0) {
                goal -= 1
                updateGoalText()
            }
        }
        binding.goalInputBox.setEndIconOnClickListener {
            goal += 1
            updateGoalText()
        }
        updateGoalText()

        builder.setPositiveButton(R.string.save, null)
        builder.setNegativeButton(R.string.cancel, null)
    }

    private fun updateGoalText() {
        if (goal > 0) {
            binding.goalInput.setText(goal.toString())
        } else {
            binding.goalInput.setText("Ø")
        }
    }

    fun forNewCounter(): CounterSettingsDialogBuilder {
        builder.setTitle(R.string.add_counter)
        binding.fakeSpinnerInterval.setText(Interval.DEFAULT.toHumanReadableResourceId())
        binding.spinnerInterval.setSelection(intervalAdapter.positionOf(Interval.DEFAULT))
        return this
    }

    fun forExistingCounter(counter: CounterSummary): CounterSettingsDialogBuilder {
        builder.setTitle(R.string.edit_counter)
        previousName = counter.name
        binding.nameEdit.setText(counter.name)
        binding.fakeSpinnerInterval.setText(counter.interval.toHumanReadableResourceId())
        binding.spinnerInterval.setSelection(intervalAdapter.positionOf(counter.interval))
        colorAdapter.selectedColor = counter.color.colorInt
        goal = counter.goal
        updateGoalText()
        return this
    }

    fun setOnSaveListener(onSave: (counterMetadata: CounterMetadata) -> Unit): CounterSettingsDialogBuilder {
        onSaveListener = onSave
        return this
    }

    fun setOnDeleteListener(onClickListener: DialogInterface.OnClickListener): CounterSettingsDialogBuilder {
        builder.setNeutralButton(R.string.delete_or_reset, onClickListener)
        return this
    }

    fun setOnDismissListener(onClickListener: DialogInterface.OnDismissListener): CounterSettingsDialogBuilder {
        builder.setOnDismissListener(onClickListener)
        return this
    }

    fun show(): AlertDialog {
        val dialog = builder.show()
        // Override the listener last (after showing) instead of passing it to setPositiveButton so we can decide when to dismiss the dialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = binding.nameEdit.text.toString().trim()
            when {
                name.isBlank() -> {
                    binding.nameEdit.error = context.getString(R.string.name_cant_be_blank)
                }
                name != previousName && viewModel.counterExists(name) -> {
                    binding.nameEdit.error = context.getString(R.string.already_exists)
                }
                else -> {
                    onSaveListener(
                        CounterMetadata(
                            name,
                            intervalAdapter.itemAt(binding.spinnerInterval.selectedItemPosition),
                            goal,
                            CounterColor(colorAdapter.selectedColor),
                        )
                    )
                    dialog.dismiss()
                }
            }
        }
        if (binding.nameEdit.text.isNullOrEmpty()) {
            binding.nameEdit.requestFocus()
            dialog.window?.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        return dialog
    }
}
