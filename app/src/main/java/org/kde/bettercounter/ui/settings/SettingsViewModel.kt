package org.kde.bettercounter.ui.settings

import android.app.Application
import org.kde.bettercounter.persistence.AverageMode
import org.kde.bettercounter.persistence.Repository

class SettingsViewModel(application: Application) {

    private val repo: Repository = Repository.create(application)

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
}