package org.kde.bettercounter

import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.kde.bettercounter.boilerplate.DragAndSwipeTouchHelper


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        viewModel = ViewModelProvider(this).get(ViewModel::class.java)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { view ->
            fab.visibility = View.GONE
            var editView = layoutInflater.inflate(R.layout.simple_edit_text, null)
            AlertDialog.Builder(view.context)
                .setView(editView)
                .setTitle("Add counter")
                .setPositiveButton("Create") { _, _ ->
                    var name = editView.findViewById<EditText>(R.id.text_edit).text.toString()
                    if (viewModel.counterExists(name)) {
                        Toast.makeText(this, R.string.already_exists, Toast.LENGTH_LONG).show()
                    } else {
                        viewModel.addCounter(name)
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                .setOnDismissListener { fab.visibility = View.VISIBLE }
                .show()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recycler)
        var entryViewAdapter  = EntryListViewAdapter(this, viewModel)
        recyclerView.adapter = entryViewAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        val callback = DragAndSwipeTouchHelper(entryViewAdapter)
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

}