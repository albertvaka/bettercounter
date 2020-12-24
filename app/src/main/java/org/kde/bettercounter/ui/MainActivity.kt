package org.kde.bettercounter.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.kde.bettercounter.EntryListViewAdapter
import org.kde.bettercounter.R
import org.kde.bettercounter.ViewModel
import org.kde.bettercounter.boilerplate.DragAndSwipeTouchHelper
import org.kde.bettercounter.boilerplate.HackyLayoutManager
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.CounterDetails
import org.kde.bettercounter.persistence.Interval
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ViewModel

    private lateinit var chart : BetterChart
    private lateinit var chartTitle : TextView
    private lateinit var chartAverage : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        viewModel = ViewModelProvider(this).get(ViewModel::class.java)


        // Views binding
        // -------------

        val fab : FloatingActionButton = findViewById(R.id.fab)
        val recyclerView : RecyclerView = findViewById(R.id.recycler)
        val bottomSheet : View = findViewById(R.id.bottomSheet)
        chartTitle = findViewById(R.id.chartTitle)
        chartAverage = findViewById(R.id.chartAverage)
        chart = findViewById(R.id.chart)


        // Bottom sheet with graph
        // -----------------------

        val sheetBehavior : BottomSheetBehavior<View> = BottomSheetBehavior.from(bottomSheet)
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        var sheetIsExpanding = false
        val sheetFoldedPadding = recyclerView.paddingBottom // padding so the fab is in view
        val sheetUnfoldedPadding = bottomSheet.layoutParams.height + 50 // padding to fit the bottomSheet

        chart.setup()

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
                    recyclerView.setPadding(0, 0, 0, bottomPadding)
                }
            }
        })


        // Create counter dialog
        // ---------------------

        fab.setOnClickListener {
            fab.visibility = View.GONE
            CounterSettingsDialogBuilder(this@MainActivity, viewModel)
                .forNewCounter()
                .setOnSaveListener { name, interval ->
                    viewModel.addCounter(name, interval)
                }
                .setOnDismissListener { fab.visibility = View.VISIBLE }
                .show()
        }


        // Counter list
        // ------------

        val entryViewAdapter = EntryListViewAdapter(this, viewModel)
        entryViewAdapter.onItemAdded = { pos -> recyclerView.smoothScrollToPosition(pos) }
        var currentChartLiveData : LiveData<CounterDetails>? = null
        entryViewAdapter.onItemClickListener = { position: Int, counter: CounterSummary ->
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            sheetIsExpanding = true
            recyclerView.setPadding(0, 0, 0, sheetUnfoldedPadding)
            recyclerView.smoothScrollToPosition(position)

            val liveData = viewModel.getCounterDetails(counter.name)
            currentChartLiveData?.removeObservers(this@MainActivity)
            currentChartLiveData = liveData
            var isChartFirstUpdate = true
            liveData.observe(this@MainActivity) {
                runOnUiThread {
                    updateChartSheet(it)
                    if (isChartFirstUpdate) {
                        chart.animateY(200)
                        isChartFirstUpdate = false
                    }
                }
            }
        }

        recyclerView.adapter = entryViewAdapter
        recyclerView.layoutManager = HackyLayoutManager(this)
        val callback = DragAndSwipeTouchHelper(entryViewAdapter)
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)

    }

    private fun updateChartSheet(counter: CounterDetails) {
        when (counter.interval) {
            Interval.DAY -> {
                chartTitle.text = counter.name + " (last 24h)"
                chartAverage.text = "Average: 3.3 times/hour"
                chart.setDailyData(counter.intervalEntries)
            }
            //...
        }
    }

}
