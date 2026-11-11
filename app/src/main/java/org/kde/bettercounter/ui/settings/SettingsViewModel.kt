package org.kde.bettercounter.ui.settings

import android.app.Application
import android.net.Uri
import java.io.IOException
import org.kde.bettercounter.persistence.Exporter
import org.kde.bettercounter.ui.widget.WidgetProvider
import org.kde.bettercounter.persistence.AverageMode
import org.kde.bettercounter.persistence.Repository

class SettingsViewModel(private val application: Application) {

    private val repo: Repository = Repository.create(application)
    private val exporter: Exporter = Exporter(application, repo)

    fun hasCounters(): Boolean = repo.getCounterList().isNotEmpty()

    // Export must be completed and the stream closed before any measurements are removed.
    suspend fun resetAllCounters(exportUri: Uri? = null) {
        if (exportUri != null) {
            val stream = application.contentResolver.openOutputStream(exportUri)
                ?: throw IOException("Could not open the selected export file")
            exporter.exportAll(stream) { }
        }
        repo.removeAllEntries()
        WidgetProvider.refreshWidgets(application)
        exporter.autoExportIfEnabled()
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

    fun isKeepScreenOnEnabled(): Boolean {
        return repo.isKeepScreenOnEnabled()
    }

    fun setKeepScreenOn(enabled: Boolean) {
        repo.setKeepScreenOn(enabled)
    }

    fun getAverageCalculationMode(): AverageMode {
        return repo.getAverageCalculationMode()
    }

    fun setAverageCalculationMode(mode: AverageMode) {
        repo.setAverageCalculationMode(mode)
    }
}