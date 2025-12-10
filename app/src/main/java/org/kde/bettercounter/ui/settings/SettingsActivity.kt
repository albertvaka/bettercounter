package org.kde.bettercounter.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.android.material.snackbar.Snackbar
import org.kde.bettercounter.R
import org.kde.bettercounter.boilerplate.CreateFileParams
import org.kde.bettercounter.boilerplate.CreateFileResultContract
import org.kde.bettercounter.databinding.ActivitySettingsBinding
import org.kde.bettercounter.persistence.AverageMode
import org.kde.bettercounter.persistence.FirstHourOfDay
import org.kde.bettercounter.ui.main.showTimePicker

class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by lazy { SettingsViewModel(application) }
    private val binding: ActivitySettingsBinding by lazy { ActivitySettingsBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.settingsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        // Auto-export
        binding.switchAutoExport.isChecked = viewModel.isAutoExportOnSaveEnabled()
        binding.switchAutoExport.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoExportOnSave(isChecked)
            if (isChecked && viewModel.getAutoExportFileUri() == null) {
                selectAutoExportFile()
            } else {
                updateAutoExportFileButtonVisibility(isChecked)
            }
        }
        binding.buttonChangeAutoExportFile.setOnClickListener {
            selectAutoExportFile()
        }
        updateAutoExportFileButtonVisibility(binding.switchAutoExport.isChecked)

        // First hour of day
        updateFirstHourOfDayText()
        binding.buttonChangeFirstHourOfDay.setOnClickListener {
            showTimePicker(this, FirstHourOfDay.get()) {
                FirstHourOfDay.set(it)
                updateFirstHourOfDayText()
            }
        }
        updateAutoExportFileButtonVisibility(binding.switchAutoExport.isChecked)

        // Average calculation mode
        when (viewModel.getAverageCalculationMode()) {
            AverageMode.FIRST_TO_NOW -> binding.radioFirstToNow.isChecked = true
            AverageMode.FIRST_TO_LAST -> binding.radioFirstToLast.isChecked = true
        }

        binding.radioGroupAverageMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioFirstToNow -> AverageMode.FIRST_TO_NOW
                R.id.radioFirstToLast -> AverageMode.FIRST_TO_LAST
                else -> AverageMode.FIRST_TO_LAST
            }
            viewModel.setAverageCalculationMode(mode)
        }
    }

    private fun updateFirstHourOfDayText() {
        val firstHourOfDay = FirstHourOfDay.get()
        binding.textFirstHourOfDay.text = getString(R.string.first_hour_of_day_text, firstHourOfDay.hour, firstHourOfDay.minute)
    }

    private fun updateAutoExportFileButtonVisibility(autoExportEnabled: Boolean) {
        val existingUri = viewModel.getAutoExportFileUri()?.toUri()
        if (autoExportEnabled && existingUri != null) {
            displayCurrentExportFileName(existingUri)
            binding.buttonChangeAutoExportFile.visibility = View.VISIBLE
        } else {
            binding.textCurrentExportFile.text = getString(R.string.export_disabled)
            binding.buttonChangeAutoExportFile.visibility = View.GONE
        }
    }

    private fun displayCurrentExportFileName(uri: Uri) {
        val fileName = getFileNameFromUri(uri) ?: uri.toString()
        binding.textCurrentExportFile.text = getString(R.string.current_export_file, fileName)
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        try {
            return when (uri.scheme) {
                "content" -> {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val displayNameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                            if (displayNameIndex != -1) {
                                cursor.getString(displayNameIndex)
                            } else null
                        } else null
                    }
                }
                "file" -> {
                    uri.lastPathSegment
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return uri.toString()
        }
    }

    private fun selectAutoExportFile() {
        val fileName = "bettercounter-auto-export.csv"
        autoExportFilePicker.launch(CreateFileParams("text/csv", fileName))
    }

    private val autoExportFilePicker: ActivityResultLauncher<CreateFileParams> = registerForActivityResult(
        CreateFileResultContract()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                viewModel.setAutoExportFileUri(uri.toString())

                displayCurrentExportFileName(uri)

                updateAutoExportFileButtonVisibility(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Snackbar.make(binding.root, getString(R.string.export_error), Snackbar.LENGTH_LONG).show()

                viewModel.setAutoExportOnSave(false)
                binding.switchAutoExport.isChecked = false
            }
        } else {
            if (viewModel.getAutoExportFileUri() == null) {
                viewModel.setAutoExportOnSave(false)
                binding.switchAutoExport.isChecked = false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}