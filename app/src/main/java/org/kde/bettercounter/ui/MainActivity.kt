package org.kde.bettercounter.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import org.kde.bettercounter.EntryListViewAdapter
import org.kde.bettercounter.R
import org.kde.bettercounter.StatsCalculator
import org.kde.bettercounter.ViewModel
import org.kde.bettercounter.boilerplate.DragAndSwipeTouchHelper
import org.kde.bettercounter.boilerplate.HackyLayoutManager
import org.kde.bettercounter.databinding.ActivityMainBinding
import org.kde.bettercounter.databinding.ProgressDialogBinding
import org.kde.bettercounter.persistence.CounterDetails
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Interval
import java.util.*


class MainActivity : AppCompatActivity() {

    private val EXPORT_REQUEST_CODE: Int = 12
    private lateinit var viewModel: ViewModel
    private lateinit var binding: ActivityMainBinding
    private var statsCalculator = StatsCalculator(this)
    private lateinit var sheetBehavior : BottomSheetBehavior<View>
    private var sheetIsExpanding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)


        viewModel = ViewModelProvider.AndroidViewModelFactory(application).create(ViewModel::class.java)

        // Bottom sheet with graph
        // -----------------------

        sheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        sheetIsExpanding = false
        val sheetFoldedPadding = binding.recycler.paddingBottom // padding so the fab is in view
        var sheetUnfoldedPadding = 0  // padding to fit the bottomSheet. The height is not hardcoded, so we have wait to have it:
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                sheetUnfoldedPadding = binding.bottomSheet.height + 50
                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        binding.chart.setup()

        sheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    sheetIsExpanding = false
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (!sheetIsExpanding) { // only do this when collapsing. when expanding we set the final padding at once so smoothScrollToPosition can do its job
                    val bottomPadding =
                        sheetFoldedPadding + ((1.0 + slideOffset) * (sheetUnfoldedPadding - sheetFoldedPadding)).toInt()
                    binding.recycler.setPadding(0, 0, 0, bottomPadding)
                }
            }
        })


        // Create counter dialog
        // ---------------------

        binding.fab.setOnClickListener {
            binding.fab.visibility = View.GONE
            CounterSettingsDialogBuilder(this@MainActivity, viewModel)
                .forNewCounter()
                .setOnSaveListener { name, interval, color ->
                    viewModel.addCounter(name, interval, color)
                }
                .setOnDismissListener { binding.fab.visibility = View.VISIBLE }
                .show()
        }


        // Counter list
        // ------------

        val entryViewAdapter = EntryListViewAdapter(this, viewModel)
        entryViewAdapter.onItemAdded = { pos -> binding.recycler.smoothScrollToPosition(pos) }
        var currentChartLiveData : LiveData<CounterDetails>? = null
        entryViewAdapter.onItemClickListener = { position: Int, counter: CounterSummary ->
            if (sheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                sheetIsExpanding = true
            }
            binding.recycler.setPadding(0, 0, 0, sheetUnfoldedPadding)
            binding.recycler.smoothScrollToPosition(position)

            val liveData = viewModel.getCounterDetails(counter.name)
            currentChartLiveData?.removeObservers(this@MainActivity)
            currentChartLiveData = liveData
            var isChartFirstUpdate = true
            liveData.observe(this@MainActivity) {
                runOnUiThread {
                    updateChartSheet(it)
                    if (isChartFirstUpdate) {
                        binding.chart.animateY(200)
                        isChartFirstUpdate = false
                    }
                }
            }
        }

        binding.recycler.adapter = entryViewAdapter
        binding.recycler.layoutManager = HackyLayoutManager(this)
        val callback = DragAndSwipeTouchHelper(entryViewAdapter)
        ItemTouchHelper(callback).attachToRecyclerView(binding.recycler)

    }

    private fun updateChartSheet(counter: CounterDetails) {
        val defaultColor = getColor(R.color.colorPrimary)
        binding.chart.setColorForNextDataSet(if (counter.color == defaultColor) getColor(R.color.colorAccent) else counter.color)
        when (counter.interval) {
            Interval.DAY -> {
                binding.chartTitle.text = getString(R.string.chart_title_daily, counter.name)
                binding.chartAverage.text = statsCalculator.getDaily(counter.intervalEntries)
                binding.chart.setDailyData(counter.intervalEntries)
            }
            Interval.WEEK -> {
                binding.chartTitle.text = getString(R.string.chart_title_weekly, counter.name)
                binding.chartAverage.text = statsCalculator.getWeekly(counter.intervalEntries)
                binding.chart.setWeeklyData(counter.intervalEntries)
            }
            Interval.MONTH -> {
                binding.chartTitle.text = getString(R.string.chart_title_monthly, counter.name)
                binding.chartAverage.text = statsCalculator.getMonthly(counter.intervalEntries)
                binding.chart.setMonthlyData(counter.intervalEntries)
            }
            Interval.YEAR -> {
                binding.chartTitle.text = getString(R.string.chart_title_yearly, counter.name)
                binding.chartAverage.text = statsCalculator.getYearly(counter.intervalEntries)
                binding.chart.setYearlyData(counter.intervalEntries)
            }
            Interval.YTD -> {
                binding.chartTitle.text = getString(R.string.chart_title_ytd, counter.name)
                binding.chartAverage.text = statsCalculator.getYtd(counter.intervalEntries)
                binding.chart.setYtdData(counter.intervalEntries)
            }
            Interval.LIFETIME -> {
                binding.chartTitle.text = getString(R.string.chart_title_lifetime, counter.name)
                binding.chartAverage.text = statsCalculator.getLifetime(counter.intervalEntries)
                binding.chart.setLifetimeData(counter.intervalEntries)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.export -> {
                startExportIntent()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startExportIntent() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_TITLE, "bettercounter-export.csv")
        startActivityForResult(intent, EXPORT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == EXPORT_REQUEST_CODE) {
            data?.data?.let { uri ->
                contentResolver.openOutputStream(uri)?.let { stream ->

                    val progressDialogBinding = ProgressDialogBinding.inflate(layoutInflater)
                    val dialog = AlertDialog.Builder(this)
                        .setView(progressDialogBinding.root)
                        .setCancelable(false)
                        .create()
                    dialog.show()

                    val progressHandler = Handler(Looper.getMainLooper()) {
                        progressDialogBinding.text.text = getString(R.string.exported_n, it.arg1, it.arg2)
                        if (it.arg1 == it.arg2) {
                            dialog.setCancelable(true)
                        }
                        true
                    }
                    viewModel.exportAll(stream, progressHandler)
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            sheetIsExpanding = false
        } else {
            super.onBackPressed()
        }
    }
}
