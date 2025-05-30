package org.kde.bettercounter.persistence

import android.app.Application
import android.widget.Toast
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.kde.bettercounter.R
import java.io.OutputStream

class Exporter(val application: Application, val repo: Repository) {

    companion object {
        private var autoExportJob : Job? = null
    }

    fun autoExportIfEnabled() {
        if (!repo.isAutoExportOnSaveEnabled()) {
            return
        }
        autoExportJob?.cancel() // Ensure we don't have two exports running in parallel
        autoExportJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val uri = repo.getAutoExportFileUri()!!.toUri()
                application.contentResolver.openOutputStream(uri, "wt")?.let { stream ->
                    exportAll(stream) { }
                }
            } catch (_: Exception) {
                repo.setAutoExportOnSave(false)
                Toast.makeText(application, R.string.export_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun exportAll(stream: OutputStream, progressCallback: (progress: Int) -> Unit) {
        stream.use {
            it.bufferedWriter().use { writer ->
                for ((i, name) in repo.getCounterList().withIndex()) {
                    progressCallback(i)
                    val entries = repo.getAllEntriesSortedByDate(name)
                    writer.write(name)
                    for (entry in entries) {
                        writer.write(",")
                        writer.write(entry.date.time.toString())
                    }
                    writer.write("\n")
                }
                progressCallback(repo.getCounterList().size)
            }
        }
    }
}