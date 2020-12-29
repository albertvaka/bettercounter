package org.kde.bettercounter.ui

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
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
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.CounterDetails
import org.kde.bettercounter.persistence.Interval
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ViewModel
    private lateinit var binding: ActivityMainBinding
    private var statsCalculator = StatsCalculator(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)


        viewModel = ViewModelProvider(this).get(ViewModel::class.java)

        // Bottom sheet with graph
        // -----------------------

        val sheetBehavior : BottomSheetBehavior<View> = BottomSheetBehavior.from(binding.bottomSheet)
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        var sheetIsExpanding = false
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
                .setOnSaveListener { name, interval ->
                    viewModel.addCounter(name, interval)
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

}