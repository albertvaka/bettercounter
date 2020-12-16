package org.kde.bettercounter

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kde.bettercounter.boilerplate.DragAndSwipeTouchHelper
import org.kde.bettercounter.boilerplate.HackyLayoutManager
import org.kde.bettercounter.persistence.Counter
import org.kde.bettercounter.persistence.Entry


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ViewModel

    private lateinit var graph : GraphView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        viewModel = ViewModelProvider(this).get(ViewModel::class.java)


        // Views
        // -------------

        val fab : FloatingActionButton = findViewById(R.id.fab)
        val recyclerView : RecyclerView = findViewById(R.id.recycler)
        graph = findViewById(R.id.graph)


        // Bottom sheet with graph
        // -----------------------

        val sheetBehavior : BottomSheetBehavior<GraphView> = BottomSheetBehavior.from(graph)
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        var sheetIsExpanding = false
        var sheetFoldedPadding = recyclerView.paddingBottom; // padding so the fab is in view
        var sheetUnfoldedPadding = graph.layoutParams.height + 50; // padding to fit the bottomSheet

        sheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    sheetIsExpanding = false;
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (!sheetIsExpanding) { // only do this when collapsing. when expanding we set the final padding at once so smoothScrollToPosition can do its job
                    var bottomPadding = sheetFoldedPadding + ((1.0 + slideOffset) * (sheetUnfoldedPadding - sheetFoldedPadding)).toInt()
                    recyclerView.setPadding(0, 0, 0, bottomPadding)
                }
            }
        })


        // Create counter dialog
        // ---------------------

        fab.setOnClickListener { view ->
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
        { position: Int, counter: Counter ->
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val entries = viewModel.getAllEntriesInCounterInterval(counter.name)
                runOnUiThread {
                    updateGraphForCounter(counter, entries)
                    sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    sheetIsExpanding = true
                    recyclerView.setPadding(0, 0, 0, sheetUnfoldedPadding)
                    recyclerView.smoothScrollToPosition(position)
                }
            }
        }
        recyclerView.adapter = entryViewAdapter
        recyclerView.layoutManager = HackyLayoutManager(this)
        val callback = DragAndSwipeTouchHelper(entryViewAdapter)
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)

    }

    fun updateGraphForCounter(c: Counter, entries: List<Entry>) {
        graph.title = c.name
        val series = mutableListOf<DataPoint>()
        for (entry in entries) {
            series.add(DataPoint(entry.date, 1.0))
        }
        graph.addSeries(LineGraphSeries(series.toTypedArray()))
    }
}
