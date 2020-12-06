package org.kde.bettercounter

import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kde.bettercounter.boilerplate.DragAndSwipeTouchHelper
import org.kde.bettercounter.persistence.Counter
import org.kde.bettercounter.persistence.Entry


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ViewModel

    private lateinit var graph : GraphView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        graph = findViewById(R.id.graph)
        val bottomSheetBehavior = BottomSheetBehavior.from(graph)

        viewModel = ViewModelProvider(this).get(ViewModel::class.java)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { view ->
            fab.visibility = View.GONE
            val editView = layoutInflater.inflate(R.layout.edit_counter, null)
            val spinner = editView.findViewById<Spinner>(R.id.interval_spinner)
            val intervalAdapter = IntervalAdapter(this)
            spinner.adapter = intervalAdapter
            AlertDialog.Builder(view.context)
                .setView(editView)
                .setTitle(R.string.add_counter)
                .setPositiveButton(R.string.save) { _, _ ->
                    val name = editView.findViewById<EditText>(R.id.text_edit).text.toString()
                    if (name.isBlank()) {
                        Toast.makeText(this, R.string.name_cant_be_blank, Toast.LENGTH_LONG).show()
                    } else if (viewModel.counterExists(name)) {
                        Toast.makeText(this, R.string.already_exists, Toast.LENGTH_LONG).show()
                    } else {
                        viewModel.addCounter(
                            name,
                            intervalAdapter.itemAt(spinner.selectedItemPosition)
                        )
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
                .setOnDismissListener { fab.visibility = View.VISIBLE }
                .show()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recycler)
        val entryViewAdapter = EntryListViewAdapter(
            this,
            viewModel
        ) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val entries = viewModel.getAllEntriesInCounterInterval(it.name)
                runOnUiThread {
                    showGraph(it, entries)
                }
            }
        }
        recyclerView.adapter = entryViewAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        val callback = DragAndSwipeTouchHelper(entryViewAdapter)
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)

    }

    fun showGraph(c: Counter, entries: List<Entry>) {
        graph.title = c.name
        val series = mutableListOf<DataPoint>()
        for (entry in entries) {
            series.add(DataPoint(entry.date, 1.0))
        }
        graph.addSeries(LineGraphSeries(series.toTypedArray()))
    }
}
