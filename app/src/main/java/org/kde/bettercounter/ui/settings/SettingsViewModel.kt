package org.kde.bettercounter.ui.settings

import android.app.Application
import android.content.Context
import org.kde.bettercounter.boilerplate.AppDatabase
import org.kde.bettercounter.persistence.AverageMode
import org.kde.bettercounter.persistence.Entry
import org.kde.bettercounter.persistence.Repository
import java.util.Date

class SettingsViewModel(val application: Application) {

    private val repo: Repository

    init {
        val db = AppDatabase.getInstance(application)
        val prefs = application.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        repo = Repository(application, db.entryDao(), prefs)
    }

    fun isAutoExportOnSaveEnabled(): Boolean {
        return repo.isAutoExportOnSaveEnabled()
    }

    fun setAutoExportOnSave(enabled: Boolean) {
        repo.setAutoExportOnSave(enabled)
    }

    fun getAutoExportFileUri(): String? {
        return repo.getAutoExportFileUri()
    }

    fun setAutoExportFileUri(uriString: String) {
        repo.setAutoExportFileUri(uriString)
    }

    fun getAverageCalculationMode(): AverageMode {
        return repo.getAverageCalculationMode()
    }

    fun setAverageCalculationMode(mode: AverageMode) {
        repo.setAverageCalculationMode(mode)
    }

    companion object {
        fun parseImportLine(line: String, namesToImport: MutableList<String>, entriesToImport: MutableList<Entry>) {
            val nameAndDates = line.splitToSequence(",").iterator()
            var name = nameAndDates.next()
            var nameEnded = false
            nameAndDates.forEach { timestamp ->
                // Hack to support counters with commas in their names
                val timestampLong = if (nameEnded) {
                    timestamp.toLong()
                } else {
                    val maybeTimestamp = timestamp.toLongOrNull()
                    if (maybeTimestamp == null || maybeTimestamp < 100000000000L) {
                        name += ",$timestamp"
                        return@forEach
                    }
                    nameEnded = true
                    maybeTimestamp
                }
                entriesToImport.add(Entry(name = name, date = Date(timestampLong)))
            }
            namesToImport.add(name)
        }
    }

}