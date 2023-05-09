package org.kde.bettercounter.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import org.kde.bettercounter.ChartsAdapter
import org.kde.bettercounter.EntryListViewAdapter
import org.kde.bettercounter.R
import org.kde.bettercounter.ViewModel
import org.kde.bettercounter.boilerplate.*
import org.kde.bettercounter.databinding.ActivityMainBinding
import org.kde.bettercounter.databinding.ProgressDialogBinding
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.DEFAULT_INTERVAL
import org.kde.bettercounter.persistence.Interval
import org.kde.bettercounter.persistence.Tutorial


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ViewModel
    private lateinit var entryViewAdapter : EntryListViewAdapter
    private lateinit var binding: ActivityMainBinding
    private lateinit var sheetBehavior : BottomSheetBehavior<View>
    private var sheetIsExpanding = false
    private var onBackPressedCloseSheetCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                this.isEnabled = false
                sheetIsExpanding = false
            }
        }
    }

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
        var sheetUnfoldedPadding = 0  // padding to fit the bottomSheet. We read it once and assume all sheets are going to be the same height
        // FIXME: Hack so the size of the sheet is known from the beginning, since we only compute it once.
        binding.charts.adapter = ChartsAdapter(this, viewModel, CounterSummary("Empty", Color.BLACK, Interval.DAY, 0, 0, null, null))
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                sheetUnfoldedPadding = binding.bottomSheet.height + 50
                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        sheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    onBackPressedCloseSheetCallback.isEnabled = true
                    sheetIsExpanding = false
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN){
                    onBackPressedCloseSheetCallback.isEnabled = false
                    setFabToCreate()
                    entryViewAdapter.clearItemSelected()
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

        onBackPressedDispatcher.addCallback(onBackPressedCloseSheetCallback)

        // Create counter dialog
        // ---------------------
        setFabToCreate()

        // Counter list
        // ------------
        entryViewAdapter = EntryListViewAdapter(this, viewModel)
        entryViewAdapter.onItemAdded = { pos -> binding.recycler.smoothScrollToPosition(pos) }
        entryViewAdapter.onItemSelected = { position: Int, counter: CounterSummary ->
            if (sheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                onBackPressedCloseSheetCallback.isEnabled = true
                sheetIsExpanding = true
            }
            binding.recycler.setPadding(0, 0, 0, sheetUnfoldedPadding)
            binding.recycler.smoothScrollToPosition(position)

            setFabToEdit(counter)

            binding.detailsTitle.text = counter.name
            val adapter = ChartsAdapter(this, viewModel, counter)
            binding.charts.swapAdapter(adapter, true)
            binding.charts.scrollToPosition(adapter.itemCount-1)
        }

        binding.recycler.adapter = entryViewAdapter
        binding.recycler.layoutManager = LinearLayoutManager(this)

        binding.charts.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false).apply {
            stackFromEnd = true
        }

        binding.charts.isNestedScrollingEnabled = false
        PagerSnapHelper().attachToRecyclerView(binding.charts) // Scroll one by one
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.export_csv -> {
                exportFilePicker.launch(CreateFileParams("text/csv","bettercounter-export.csv"))
                true
            }
            R.id.import_csv -> {
                importFilePicker.launch(OpenFileParams("text/*"))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val importFilePicker : ActivityResultLauncher<OpenFileParams> = registerForActivityResult(OpenFileResultContract()) { uri: Uri? ->
        if (uri != null) {
            contentResolver.openInputStream(uri)?.let { stream ->

                var hasImported = false

                val progressDialogBinding = ProgressDialogBinding.inflate(layoutInflater)
                val dialog = AlertDialog.Builder(this)
                    .setView(progressDialogBinding.root)
                    .setCancelable(false)
                    .setOnDismissListener {
                        if (hasImported) {
                            // restart app
                            val intent = Intent(this, MainActivity::class.java)
                            this.startActivity(intent)
                            finishAffinity()
                        }
                    }
                    .create()
                dialog.show()

                val progressHandler = Handler(Looper.getMainLooper()) {
                    if (it.arg2 == -1) {
                        progressDialogBinding.text.text = getString(R.string.import_error)
                        dialog.setCancelable(true)
                    } else {
                        progressDialogBinding.text.text = getString(R.string.imported_n, it.arg1)
                        if (it.arg2 == 1) { // we are done
                            dialog.setCancelable(true)
                            hasImported = true
                            // Hide all tutorials
                            Tutorial.values().forEach { tuto -> viewModel.setTutorialShown(tuto) }
                        }
                    }
                    true
                }
                val defaultColor = resources.obtainTypedArray(R.array.picker_colors).getColor(0, 0)
                viewModel.importAll(stream, progressHandler, DEFAULT_INTERVAL, defaultColor)
            }
        }
    }

    private val exportFilePicker : ActivityResultLauncher<CreateFileParams> = registerForActivityResult(CreateFileResultContract()) { uri: Uri? ->
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.let { stream ->

                val progressDialogBinding = ProgressDialogBinding.inflate(layoutInflater)
                val dialog = AlertDialog.Builder(this)
                    .setView(progressDialogBinding.root)
                    .setCancelable(false)
                    .create()
                dialog.show()

                val progressHandler = Handler(Looper.getMainLooper()) {
                    progressDialogBinding.text.text =
                        getString(R.string.exported_n, it.arg1, it.arg2)
                    if (it.arg1 == it.arg2) {
                        dialog.setCancelable(true)
                    }
                    true
                }
                viewModel.exportAll(stream, progressHandler)
            }
        }
    }

    private fun setFabToCreate() {
        binding.fab.setImageResource(R.drawable.ic_add)
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
    }

    private fun setFabToEdit(counter : CounterSummary) {
        binding.fab.setImageResource(R.drawable.ic_edit)
        binding.fab.setOnClickListener {
            binding.fab.visibility = View.GONE

            CounterSettingsDialogBuilder(this@MainActivity, viewModel)
                .forExistingCounter(counter)
                .setOnSaveListener { newName, newInterval, newColor ->
                    if (counter.name != newName) {
                        viewModel.editCounter(counter.name, newName, newInterval, newColor)
                    } else {
                        viewModel.editCounterSameName(newName, newInterval, newColor)
                    }
                    // We are not subscribed to the summary livedata, so we won't get notified of the change we just made.
                    // Update our local copy so it has the right data if we open the dialog again.
                    counter.name = newName
                    counter.interval = newInterval
                    counter.color = newColor
                }
                .setOnDismissListener {
                    binding.fab.visibility = View.VISIBLE
                }
                .setOnDeleteListener { _, _ ->
                    AlertDialog.Builder(this)
                        .setTitle(counter.name)
                        .setMessage(R.string.delete_confirmation)
                        .setNeutralButton(R.string.reset) { _, _ ->
                            viewModel.resetCounter(counter.name)
                            sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                            onBackPressedCloseSheetCallback.isEnabled = false
                            sheetIsExpanding = false
                        }
                        .setPositiveButton(R.string.delete) { _, _ ->
                            viewModel.deleteCounter(counter.name)
                            sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                            onBackPressedCloseSheetCallback.isEnabled = false
                            sheetIsExpanding = false
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
                .show()
        }
    }
}
