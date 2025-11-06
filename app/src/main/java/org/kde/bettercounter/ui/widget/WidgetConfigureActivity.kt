package org.kde.bettercounter.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kde.bettercounter.R
import org.kde.bettercounter.databinding.WidgetConfigureBinding

class WidgetConfigureActivity : AppCompatActivity() {

    private val viewModel: WidgetViewModel by lazy { WidgetViewModel(application) }
    private val binding: WidgetConfigureBinding by lazy { WidgetConfigureBinding.inflate(layoutInflater) }

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        val counterNames = viewModel.getCounterList()
        if (counterNames.isEmpty()) {
            Toast.makeText(this, R.string.no_counters, Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.counterNamesList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, counterNames)
        binding.counterNamesList.setOnItemClickListener { _, _, position, _ ->

            val counterName = counterNames[position]
            WidgetViewModel.saveWidgetCounterNamePref(this, appWidgetId, counterName)

            val appWidgetManager = AppWidgetManager.getInstance(this)

            CoroutineScope(Dispatchers.Main).launch {
                WidgetProvider.updateAppWidget(application, viewModel, appWidgetManager, appWidgetId)
            }

            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}
