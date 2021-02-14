package org.kde.bettercounter.ui

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.WindowManager.LayoutParams
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import org.kde.bettercounter.ColorAdapter
import org.kde.bettercounter.IntervalAdapter
import org.kde.bettercounter.R
import org.kde.bettercounter.ViewModel
import org.kde.bettercounter.databinding.EditCounterBinding
import org.kde.bettercounter.persistence.DEFAULT_INTERVAL
import org.kde.bettercounter.persistence.Interval

class CounterSettingsDialogBuilder(private val context : Context, private val viewModel : ViewModel) {

    private val builder : AlertDialog.Builder = AlertDialog.Builder(context)
    private val binding : EditCounterBinding = EditCounterBinding.inflate(LayoutInflater.from(context))
    private val intervalAdapter = IntervalAdapter(context)
    private val colorAdapter = ColorAdapter(context)
    private var onSaveListener: (name : String, Interval, color: Int) -> Unit = { _, _,_ -> }
    private var previousName : String? = null

    init {
        builder.setView(binding.root)

        binding.spinnerInterval.adapter = intervalAdapter

        binding.colorpicker.adapter = colorAdapter
        binding.colorpicker.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        builder.setPositiveButton(R.string.save, null)
        builder.setNegativeButton(R.string.cancel, null)
    }

    fun forNewCounter() : CounterSettingsDialogBuilder {
        builder.setTitle(R.string.add_counter)
        binding.spinnerInterval.setSelection(intervalAdapter.positionOf(DEFAULT_INTERVAL))
        return this
    }

    fun forExistingCounter(name: String, interval: Interval, color: Int) : CounterSettingsDialogBuilder {
        builder.setTitle(R.string.edit_counter)
        binding.editText.setText(name)
        binding.spinnerInterval.setSelection(intervalAdapter.positionOf(interval))
        colorAdapter.selectedColor = color
        previousName = name
        return this
    }

    fun setOnSaveListener(onSave: (newName: String, newInterval: Interval, newColor: Int) -> Unit): CounterSettingsDialogBuilder {
        onSaveListener = onSave
        return this
    }

    fun setOnDeleteListener(onClickListener: DialogInterface.OnClickListener): CounterSettingsDialogBuilder {
        builder.setNeutralButton(R.string.delete, onClickListener)
        return this
    }

    fun setOnDismissListener(onClickListener: DialogInterface.OnDismissListener): CounterSettingsDialogBuilder {
        builder.setOnDismissListener(onClickListener)
        return this
    }

    fun show() : AlertDialog {
        val dialog = builder.show()
        // Override the listener last (after showing) instead of passing it to setPositiveButton so we can decide when to dismiss the dialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = binding.editText.text.toString().trim()
            when {
                name.isBlank() -> {
                    Toast.makeText(
                        context,
                        R.string.name_cant_be_blank,
                        Toast.LENGTH_LONG
                    ).show()
                }
                name != previousName && viewModel.counterExists(name) -> {
                    Toast.makeText(
                        context,
                        R.string.already_exists,
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    onSaveListener(name, intervalAdapter.itemAt(binding.spinnerInterval.selectedItemPosition), colorAdapter.selectedColor)
                    dialog.dismiss()
                }
            }
        }
        if (binding.editText.text.isEmpty()) {
            binding.editText.requestFocus()
            dialog.window?.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        return dialog
    }
}