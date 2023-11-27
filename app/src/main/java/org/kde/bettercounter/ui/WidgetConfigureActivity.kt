package org.kde.bettercounter.ui

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.kde.bettercounter.BetterApplication
import org.kde.bettercounter.R
import org.kde.bettercounter.ViewModel
import org.kde.bettercounter.databinding.WidgetConfigureBinding


class WidgetConfigureActivity : AppCompatActivity() {

    private lateinit var viewModel: ViewModel
    private lateinit var binding: WidgetConfigureBinding

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Cancel widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        viewModel = (application as BetterApplication).viewModel
        val counterNames = viewModel.getCounterList()
        if (counterNames.isEmpty()) {
            Toast.makeText(this, R.string.no_counters, Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.counterNamesList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, counterNames)
        binding.counterNamesList.setOnItemClickListener  { _, _, position, _ ->

            val counterName = counterNames[position]
            saveWidgetCounterNamePref(this, appWidgetId, counterName)

            val appWidgetManager = AppWidgetManager.getInstance(this)
            updateAppWidget(this, viewModel, appWidgetManager, appWidgetId)

            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}

private const val PREFS_NAME = "org.kde.bettercounter.ui.WidgetProvider"
private const val PREF_PREFIX_KEY = "appwidget_"

internal fun saveWidgetCounterNamePref(context: Context, appWidgetId: Int, counterName: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
    prefs.putString(PREF_PREFIX_KEY + appWidgetId, counterName)
    prefs.apply()
}

internal fun loadWidgetCounterNamePref(context: Context, appWidgetId: Int): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(PREF_PREFIX_KEY + appWidgetId, null)
        ?: throw NoSuchElementException("Counter preference not found for widget id: $appWidgetId")
}

internal fun deleteWidgetCounterNamePref(context: Context, appWidgetId: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
    prefs.remove(PREF_PREFIX_KEY + appWidgetId)
    prefs.apply()
}

internal fun existsWidgetCounterNamePref(context: Context, appWidgetId: Int) : Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.contains(PREF_PREFIX_KEY + appWidgetId)
}
