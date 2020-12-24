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
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.BarGraphSeries
import com.jjoe64.graphview.series.DataPoint
import org.kde.bettercounter.EntryListViewAdapter
import org.kde.bettercounter.R
import org.kde.bettercounter.ViewModel
import org.kde.bettercounter.boilerplate.DragAndSwipeTouchHelper
import org.kde.bettercounter.boilerplate.HackyLayoutManager
import org.kde.bettercounter.persistence.Counter
import org.kde.bettercounter.persistence.CounterEntries
import org.kde.bettercounter.persistence.Interval
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ViewModel

    private lateinit var graph : GraphView
    private lateinit var graphTitle : TextView
    private lateinit var graphAverage : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        viewModel = ViewModelProvider(this).get(ViewModel::class.java)


        // Views
        // -------------

        val fab : FloatingActionButton = findViewById(R.id.fab)
        val recyclerView : RecyclerView = findViewById(R.id.recycler)
        val bottomSheet : View = findViewById(R.id.bottomSheet)
        graphTitle = findViewById(R.id.graphTitle)
        graphAverage = findViewById(R.id.graphAverage)
        graph = findViewById(R.id.graph)


        // Bottom sheet with graph
        // -----------------------

        val sheetBehavior : BottomSheetBehavior<View> = BottomSheetBehavior.from(bottomSheet)
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        var sheetIsExpanding = false
        val sheetFoldedPadding = recyclerView.paddingBottom // padding so the fab is in view
        val sheetUnfoldedPadding = bottomSheet.layoutParams.height + 50 // padding to fit the bottomSheet

        // Graph aspect
        graph.viewport.isScalable = true
        graph.viewport.isScrollable = true
        graph.gridLabelRenderer.numHorizontalLabels = 2
        graph.gridLabelRenderer.verticalLabelsColor = getColor(R.color.colorAccent)
        graph.gridLabelRenderer.horizontalLabelsColor = getColor(R.color.colorAccent)
        graph.gridLabelRenderer.gridColor = getColor(R.color.colorPrimary)
        graph.gridLabelRenderer.setHumanRounding(false, true)
        graph.gridLabelRenderer.reloadStyles()

        sheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    sheetIsExpanding = false
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (!sheetIsExpanding) { // only do this when collapsing. when expanding we set the final padding at once so smoothScrollToPosition can do its job
                    val bottomPadding = sheetFoldedPadding + ((1.0 + slideOffset) * (sheetUnfoldedPadding - sheetFoldedPadding)).toInt()
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
        var currentGraphLiveData : LiveData<CounterEntries>? = null
        entryViewAdapter.onItemClickListener = { position: Int, counter: Counter ->
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            sheetIsExpanding = true
            recyclerView.setPadding(0, 0, 0, sheetUnfoldedPadding)
            recyclerView.smoothScrollToPosition(position)

            val liveData = viewModel.getAllEntriesInCounterInterval(counter.name)
            currentGraphLiveData?.removeObservers(this@MainActivity)
            currentGraphLiveData = liveData
            var isGraphFirstUpdate = true
            liveData.observe(this@MainActivity) {
                runOnUiThread {
                    updateGraphForCounter(it, isGraphFirstUpdate)
                    isGraphFirstUpdate = false
                }
            }
        }

        recyclerView.adapter = entryViewAdapter
        recyclerView.layoutManager = HackyLayoutManager(this)
        val callback = DragAndSwipeTouchHelper(entryViewAdapter)
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)

    }

    private fun updateGraphForCounter(counterWithEntries: CounterEntries, isFirstUpdate : Boolean) {
        graphTitle.text = counterWithEntries.name + " (last 24h)"
        graphAverage.text = "Average: 3.3 times/hour"

        graph.removeAllSeries()
        if (counterWithEntries.interval != Interval.DAY) return // TODO: Add other intervals

        if (counterWithEntries.entries.isEmpty()) {
            return
        }

        val millisInHour = 60*60*1000L
        val nowHour = System.currentTimeMillis()/millisInHour
        var maxCount = 0

        val counts : MutableMap<Long, Int> = mutableMapOf()
        for (h in (nowHour-23)..nowHour) { // Create buckets for the last 24 hours
            counts[h] = 0
        }
        for (entry in counterWithEntries.entries) {
            val hour = entry.date.time/millisInHour
            counts[hour] = counts.getOrDefault(hour, 0) + 1
        }

        val series : MutableList<DataPoint> = mutableListOf()
        for (time in counts.keys.sorted()) {
            val count = counts.getOrDefault(time, 0)
            if (count > maxCount) {
                maxCount = count
            }
            val relTime: Long = (time - nowHour)
            //Log.e("DATAPOINT", "$relTime = $count")
            series.add(DataPoint(relTime.toDouble(), count.toDouble()))
        }


        graph.gridLabelRenderer.labelFormatter = object : DefaultLabelFormatter() {
            override fun formatLabel(value: Double, isAxisX: Boolean): String {
                return if (isAxisX) {
                    var hour = Calendar.getInstance().get(Calendar.HOUR)+value.toInt()
                    if (hour < 0) hour += 24
                    hour.toString()
                } else {
                    value.toInt().toString()
                }
            }
        }

        graph.viewport.isXAxisBoundsManual = true
        graph.viewport.isYAxisBoundsManual = true
        graph.viewport.setMaxX(0.5)
        graph.viewport.setMinX(-24.4)
        graph.viewport.setMinY(0.0)
        graph.viewport.setMaxY(maxCount.toDouble())

        val barGraph = BarGraphSeries(series.toTypedArray())
        barGraph.spacing = 20
        barGraph.isAnimated = isFirstUpdate
        graph.addSeries(barGraph)
    }

}
