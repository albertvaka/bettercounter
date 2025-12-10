package org.kde.bettercounter.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kde.bettercounter.R
import org.kde.bettercounter.boilerplate.CreateFileParams
import org.kde.bettercounter.boilerplate.CreateFileResultContract
import org.kde.bettercounter.boilerplate.OpenFileParams
import org.kde.bettercounter.boilerplate.OpenFileResultContract
import org.kde.bettercounter.boilerplate.hideKeyboard
import org.kde.bettercounter.boilerplate.isKeyboardVisible
import org.kde.bettercounter.databinding.ActivityMainBinding
import org.kde.bettercounter.databinding.ProgressDialogBinding
import org.kde.bettercounter.extensions.dpToPx
import org.kde.bettercounter.persistence.CounterSummary
import org.kde.bettercounter.persistence.Interval
import org.kde.bettercounter.persistence.Tutorial
import org.kde.bettercounter.ui.chart.ChartHolder
import org.kde.bettercounter.ui.chart.ChartsAdapter
import org.kde.bettercounter.ui.editdialog.CounterSettingsDialogBuilder
import org.kde.bettercounter.ui.settings.SettingsActivity
import org.kde.bettercounter.ui.widget.WidgetProvider
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    companion object {
        const val ACTION_REFRESH_COUNTER = "org.kde.bettercounter.MainActivity.REFRESH_COUNTER"
        const val EXTRA_COUNTER_NAME = "counterName"
    }

    private val viewModel: MainActivityViewModel by lazy { MainActivityViewModel(application) }
    internal val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var entryViewAdapter: EntryListViewAdapter
    private lateinit var sheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var searchMenuItem: MenuItem
    private var extraBottomPaddingForNavigationInset = 0
    private var intervalOverride: Interval? = null
    private var sheetIsExpanding = false
    private val onBackPressedCloseSheetCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                hideBottomSheet()
            }
        }
    }
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.refreshCounter(intent.getStringExtra(EXTRA_COUNTER_NAME)!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setAndroid15insets()

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

            override fun onSlide(bottomSheet: View, buggySlideOffset: Float) {
                // Only do this when collapsing. When expanding we set the final padding at once so
                // `smoothScrollToPosition` can do its job.
                if (!sheetIsExpanding) {
                    // According to the docs, `slideOffset` is in the range [0, 1] when the sheet
                    // is expanding and [-1, 0] when it is collapsing. Here it's always negative.
                    // The `max` fixes a bug in the BottomSheet where the numbers becomes smaller
                    // than -1 if the sheet is collapsing at the same time the keyboard is expanding.
                    val slideOffset = max(-1f, buggySlideOffset)
                    val sheetPadding = sheetFoldedPadding + ((1.0 + slideOffset) * (sheetUnfoldedPadding - sheetFoldedPadding)).toInt()
                    val navigationPadding = extraBottomPaddingForNavigationInset - ((1.0 + slideOffset) * extraBottomPaddingForNavigationInset).toInt()
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
            override fun cancelFilter() {
                val searchView = searchMenuItem.actionView as SearchView
                searchView.setQuery("", false)
                searchMenuItem.collapseActionView()
            }

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
                        lifecycleScope.launch {
                            // Never show both the keyboard and the bottom sheet at the same time
                            // This could happen when we come form renaming a counter, or from the filter.
                            if (isKeyboardVisible(binding.root)) {
                                hideKeyboard(binding.root)
                                // HACK so the keyboard has time to hide before the panel expands.
                                delay(100)
                            }
                            // We need to clear the focus so it triggers the onQueryTextFocusChangeListener  the next time it's clicked
                            (searchMenuItem.actionView as SearchView).clearFocus()

                            if (sheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                                onBackPressedCloseSheetCallback.isEnabled = true
                                sheetIsExpanding = true
                            }
                            // Set the state unconditionally because it re-triggers calculating the expanded offset.
                            // Needed because the height of the sheet contents depend on whether the stats take one or two lines of text.
                            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                            sheetUnfoldedPadding = binding.bottomSheet.height
                            binding.recycler.smoothScrollToPosition(position)
                            val bottomPadding = sheetUnfoldedPadding + extraBottomPaddingForSnackbar
                            binding.recycler.setPadding(0, 0, 0, bottomPadding)
                        }
                    }
                )
                binding.charts.swapAdapter(adapter, true)
                binding.charts.scrollToPosition(adapter.itemCount - 1) // Select the latest chart
            }

            override fun onItemSelected(position: Int, counter: CounterSummary) {
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

        // Just in case it's not yet scheduled (happens when installing from Android Studio)
        WidgetProvider.scheduleHourlyUpdate(this)

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(ACTION_REFRESH_COUNTER))
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(broadcastReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        searchMenuItem = menu.findItem(R.id.action_search)
        val searchView = searchMenuItem.actionView as SearchView

        searchView.setOnQueryTextFocusChangeListener { _, focus ->
            if (focus) {
                hideBottomSheet()
            } else if (searchView.query.isNullOrEmpty()) {
                searchMenuItem.collapseActionView()
            }
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus() // to hide the cursor since the keyboard is no longer open
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                entryViewAdapter.applyFilter(newText.orEmpty())
                return true
            }
        })

        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                entryViewAdapter.applyFilter("") // Clear filter when search is closed
                return true
            }
        })

        return true
    }

    fun showChangeGraphIntervalTutorial(onDismissListener: SimpleTooltip.OnDismissListener? = null) {
        val adapter = binding.charts.adapter ?: return
        binding.charts.scrollToPosition(adapter.itemCount - 1)
        val holder = binding.charts.findViewHolderForAdapterPosition(adapter.itemCount - 1) as ChartHolder
        holder.showChangeGraphIntervalTutorial(onDismissListener)
        viewModel.setTutorialShown(Tutorial.CHANGE_GRAPH_INTERVAL)
    }

    val activityLauncherWithCountersRefresh = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        lifecycleScope.launch {
            viewModel.refreshAllCounters()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.export_csv -> {
                exportFilePicker.launch(CreateFileParams("text/csv", "bettercounter-export.csv"))
            }
            R.id.import_csv -> {
                importFilePicker.launch(OpenFileParams("text/*"))
            }
            R.id.settings -> {
                activityLauncherWithCountersRefresh.launch(Intent(this, SettingsActivity::class.java))
            }
            R.id.show_tutorial -> {
                if (viewModel.getCounterList().isEmpty()) {
                    Snackbar.make(binding.recycler, getString(R.string.no_counters), Snackbar.LENGTH_LONG).show()
                } else {
                    binding.recycler.scrollToPosition(0)
                    val holder = binding.recycler.findViewHolderForAdapterPosition(0) as EntryViewHolder
                    entryViewAdapter.showDragTutorial(holder) {
                        entryViewAdapter.showPickDateTutorial(holder) {
                            viewModel.unsetTutorialShown(Tutorial.CHANGE_GRAPH_INTERVAL)
                            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                                // Re-trigger the tutorial on the selected counter
                                showChangeGraphIntervalTutorial()
                            } else {
                                // Select the first counter
                                val firstCounterName = viewModel.getCounterList().firstOrNull()
                                    ?: return@showPickDateTutorial
                                val counter = viewModel.getCounterSummary(firstCounterName).value
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

                    // Disable all tutorials, they can cause problems when we update all the flows at once
                    Tutorial.entries.forEach { tutorial -> viewModel.setTutorialShown(tutorial) }

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
                    // We are not subscribed to the summary flow, so we won't get notified of the change we just made.
                    // Update our local copy so it has the right data if we open the dialog again.
                    counter.name = newCounterMetadata.name
                    counter.interval = newCounterMetadata.interval
                    counter.color = newCounterMetadata.color
                    counter.goal = newCounterMetadata.goal
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
                            hideBottomSheet()
                        }
                        .setPositiveButton(R.string.delete) { _, _ ->
                            viewModel.deleteCounter(counter.name)
                            hideBottomSheet()
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
                .show()
        }
    }

    private fun hideBottomSheet() {
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        onBackPressedCloseSheetCallback.isEnabled = false
        sheetIsExpanding = false
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
