package org.kde.bettercounter.ui

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.kde.bettercounter.BetterApplication
import org.kde.bettercounter.ChartsAdapter
import org.kde.bettercounter.EntryListViewAdapter
import org.kde.bettercounter.R
import org.kde.bettercounter.ViewModel
import org.kde.bettercounter.boilerplate.CreateFileParams
import org.kde.bettercounter.boilerplate.CreateFileResultContract
import org.kde.bettercounter.boilerplate.OpenFileParams
import org.kde.bettercounter.boilerplate.OpenFileResultContract
import org.kde.bettercounter.databinding.ActivityMainBinding
import org.kde.bettercounter.databinding.ProgressDialogBinding
import org.kde.bettercounter.extensions.dpToPx
import org.kde.bettercounter.extensions.millisecondsUntilNextHour
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Interval
import org.kde.bettercounter.persistence.Tutorial

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ViewModel
    private lateinit var entryViewAdapter: EntryListViewAdapter
    internal lateinit var binding: ActivityMainBinding
    private lateinit var sheetBehavior: BottomSheetBehavior<LinearLayout>
    private var extraBottomPaddingForNavigationInset = 0
    private var intervalOverride: Interval? = null
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
        setAndroid15insets()

        viewModel = (application as BetterApplication).viewModel

        // Bottom sheet with graph
        // -----------------------
        sheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        sheetIsExpanding = false
        val extraBottomPaddingForSnackbar = 100.dpToPx(this)
        val sheetFoldedPadding = binding.recycler.paddingBottom - extraBottomPaddingForSnackbar - extraBottomPaddingForNavigationInset // padding so the fab is in view
        var sheetUnfoldedPadding = sheetFoldedPadding // padding to fit the bottomSheet, to be updated later

        sheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    onBackPressedCloseSheetCallback.isEnabled = true
                    sheetIsExpanding = false
                    if (!viewModel.isTutorialShown(Tutorial.CHANGE_GRAPH_INTERVAL)) {
                        showChangeGraphIntervalTutorial()
                    }
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    onBackPressedCloseSheetCallback.isEnabled = false
                    setFabToCreate()
                    entryViewAdapter.clearItemSelected()
                }
            }

            override fun onSlide(bottomSheet: View, negativeSlideOffset: Float) {
                if (!sheetIsExpanding) { // only do this when collapsing. when expanding we set the final padding at once so smoothScrollToPosition can do its job
                    val sheetPadding = sheetFoldedPadding + ((1.0 + negativeSlideOffset) * (sheetUnfoldedPadding - sheetFoldedPadding)).toInt()
                    val navigationPadding = extraBottomPaddingForNavigationInset - ((1.0 + negativeSlideOffset) * extraBottomPaddingForNavigationInset).toInt()
                    val bottomPadding = sheetPadding + extraBottomPaddingForSnackbar + navigationPadding
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
        entryViewAdapter = EntryListViewAdapter(this, viewModel, object : EntryListViewAdapter.EntryListObserver {
            override fun onItemAdded(position: Int) {
                binding.recycler.smoothScrollToPosition(position)
            }
            override fun onSelectedItemUpdated(position: Int, counter: CounterSummary) {
                binding.detailsTitle.text = counter.name
                val interval = intervalOverride ?: counter.interval.toChartDisplayableInterval()
                val adapter = ChartsAdapter(this@MainActivity, viewModel, counter, interval,
                    onIntervalChange = { newInterval ->
                        intervalOverride = newInterval
                        onSelectedItemUpdated(position, counter)
                    },
                    onDateChange = { newDate ->
                        val chartPosition = findPositionForRangeStart(newDate)
                        binding.charts.scrollToPosition(chartPosition)
                    },
                    onDataDisplayed = {
                        // Re-triggers calculating the expanded offset, since the height of the sheet
                        // contents depend on whether the stats take one or two lines of text
                        sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        sheetUnfoldedPadding = binding.bottomSheet.height
                        binding.recycler.smoothScrollToPosition(position)
                        val bottomPadding = sheetUnfoldedPadding + extraBottomPaddingForSnackbar
                        binding.recycler.setPadding(0, 0, 0, bottomPadding)
                    }
                )
                binding.charts.swapAdapter(adapter, true)
                binding.charts.scrollToPosition(adapter.itemCount - 1) // Select the latest chart
            }
            override fun onItemSelected(position: Int, counter: CounterSummary) {
                if (sheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                    sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    onBackPressedCloseSheetCallback.isEnabled = true
                    sheetIsExpanding = true
                }

                setFabToEdit(counter)

                intervalOverride = null

                onSelectedItemUpdated(position, counter)
            }
        })
        binding.recycler.adapter = entryViewAdapter
        binding.recycler.layoutManager = LinearLayoutManager(this)

        binding.charts.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false).apply {
            stackFromEnd = true
        }

        binding.charts.isNestedScrollingEnabled = false
        PagerSnapHelper().attachToRecyclerView(binding.charts) // Scroll one by one

        // For some reason, when the app is installed via Android Studio, the broadcast that
        // refreshes the widgets after installing doesn't trigger. Do it manually here.
        forceRefreshWidgets(this)

        startRefreshEveryHourBoundary()
    }

    private fun startRefreshEveryHourBoundary() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(millisecondsUntilNextHour())
                viewModel.refreshAllObservers()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    fun showChangeGraphIntervalTutorial(onDismissListener: SimpleTooltip.OnDismissListener? = null) {
        val adapter = binding.charts.adapter ?: return
        binding.charts.scrollToPosition(adapter.itemCount - 1)
        val holder = binding.charts.findViewHolderForAdapterPosition(adapter.itemCount - 1) as ChartHolder
        holder.showChangeGraphIntervalTutorial(onDismissListener)
        viewModel.setTutorialShown(Tutorial.CHANGE_GRAPH_INTERVAL)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.export_csv -> {
                exportFilePicker.launch(CreateFileParams("text/csv", "bettercounter-export.csv"))
            }
            R.id.import_csv -> {
                importFilePicker.launch(OpenFileParams("text/*"))
            }
            R.id.show_tutorial -> {
                if (viewModel.getCounterList().isEmpty()) {
                    Snackbar.make(binding.recycler, getString(R.string.no_counters), Snackbar.LENGTH_LONG).show()
                } else {
                    binding.recycler.scrollToPosition(0)
                    val holder = binding.recycler.findViewHolderForAdapterPosition(0) as EntryViewHolder
                    entryViewAdapter.showDragTutorial(holder) {
                        entryViewAdapter.showPickDateTutorial(holder) {
                            viewModel.resetTutorialShown(Tutorial.CHANGE_GRAPH_INTERVAL)
                            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                                // Re-trigger the tutorial on the selected counter
                                showChangeGraphIntervalTutorial()
                            } else {
                                // Select the first counter
                                val firstCounterName = viewModel.getCounterList().firstOrNull()
                                    ?: return@showPickDateTutorial
                                val counter = viewModel.getCounterSummary(firstCounterName).value
                                    ?: return@showPickDateTutorial
                                entryViewAdapter.selectCounter(counter)
                            }
                        }
                    }
                }
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private val importFilePicker: ActivityResultLauncher<OpenFileParams> = registerForActivityResult(
        OpenFileResultContract()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.let { stream ->
                    val progressDialogBinding = ProgressDialogBinding.inflate(layoutInflater)
                    val dialog = MaterialAlertDialogBuilder(this)
                        .setView(progressDialogBinding.root)
                        .setCancelable(false)
                        .create()
                    dialog.show()

                    viewModel.importAll(this, stream) { progress, status ->
                        runOnUiThread {
                            if (status == -1) {
                                progressDialogBinding.text.text = getString(R.string.import_error)
                                dialog.setCancelable(true)
                            } else {
                                progressDialogBinding.text.text =
                                    getString(R.string.imported_n, progress)
                                if (status == 1) { // we are done
                                    dialog.setCancelable(true)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val exportFilePicker: ActivityResultLauncher<CreateFileParams> = registerForActivityResult(
        CreateFileResultContract()
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.let { stream ->

                val progressDialogBinding = ProgressDialogBinding.inflate(layoutInflater)
                val dialog = MaterialAlertDialogBuilder(this)
                    .setView(progressDialogBinding.root)
                    .setCancelable(false)
                    .create()
                dialog.show()

                val total = viewModel.getCounterList().size
                viewModel.exportAll(stream) { progress ->
                    runOnUiThread {
                        progressDialogBinding.text.text =
                            getString(R.string.exported_n, progress, total)
                        if (progress == total) {
                            dialog.setCancelable(true)
                        }
                    }
                }
            }
        }
    }

    private fun setFabToCreate() {
        binding.fab.setImageResource(R.drawable.ic_add)
        binding.fab.setOnClickListener {
            binding.fab.visibility = View.GONE
            CounterSettingsDialogBuilder(this@MainActivity, viewModel)
                .forNewCounter()
                .setOnSaveListener { counterMetadata ->
                    viewModel.addCounter(counterMetadata)
                }
                .setOnDismissListener { binding.fab.visibility = View.VISIBLE }
                .show()
        }
    }

    private fun setFabToEdit(counter: CounterSummary) {
        binding.fab.setImageResource(R.drawable.ic_edit)
        binding.fab.setOnClickListener {
            binding.fab.visibility = View.GONE

            CounterSettingsDialogBuilder(this@MainActivity, viewModel)
                .forExistingCounter(counter)
                .setOnSaveListener { newCounterMetadata ->
                    if (counter.name != newCounterMetadata.name) {
                        viewModel.editCounter(counter.name, newCounterMetadata)
                    } else {
                        viewModel.editCounterSameName(newCounterMetadata)
                    }
                    // We are not subscribed to the summary livedata, so we won't get notified of the change we just made.
                    // Update our local copy so it has the right data if we open the dialog again.
                    counter.name = newCounterMetadata.name
                    counter.interval = newCounterMetadata.interval
                    counter.color = newCounterMetadata.color
                }
                .setOnDismissListener {
                    binding.fab.visibility = View.VISIBLE
                }
                .setOnDeleteListener { _, _ ->
                    MaterialAlertDialogBuilder(this)
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
                            removeWidgets(this, counter.name)
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
                .show()
        }
    }

    private fun setAndroid15insets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(insets.left, insets.top, insets.right, 0)
            WindowInsetsCompat.CONSUMED
        }
        val (originalFabLeftMargin, originalFabBottomMargin, originalFabRightMargin) =
            with (binding.fab.layoutParams as ViewGroup.MarginLayoutParams) {
                Triple(leftMargin, rightMargin, bottomMargin)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.fab) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin =  originalFabLeftMargin + insets.left
                bottomMargin = originalFabBottomMargin + insets.bottom
                rightMargin = originalFabRightMargin + insets.right
            }
            if (extraBottomPaddingForNavigationInset == 0) {
                extraBottomPaddingForNavigationInset = insets.bottom
                binding.recycler.setPadding(0, 0, 0, extraBottomPaddingForNavigationInset + binding.recycler.paddingBottom)
            }
            WindowInsetsCompat.CONSUMED
        }
        val originalChartsBottomPadding = binding.charts.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.charts) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = originalChartsBottomPadding + insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.snackbar) { v, _ ->
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = 0
            }
            WindowInsetsCompat.CONSUMED
        }
    }
}
