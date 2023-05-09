package org.kde.bettercounter.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.View
import org.kde.bettercounter.R
import java.lang.ref.WeakReference
import kotlin.math.abs

/**
 * A `TextView` that, given a reference time, renders that time as a time period relative to the current time.
 * @author Based on RelativeTimeTextView by Kiran Rao
 */
open class BetterRelativeTimeTextView : androidx.appcompat.widget.AppCompatTextView {
    var referenceTime: Long = -1L
        set(value) {
            field = value

            // Note that this method could be called when a row in a ListView is recycled.
            // Hence, we need to first stop any currently running schedules (for example from the recycled view.
            stopTaskForPeriodicallyUpdatingRelativeTime()

            //Instantiate a new runnable with the new reference time
            initUpdateTimeTask()

            // Start a new schedule.
            startTaskForPeriodicallyUpdatingRelativeTime()

            // Finally, update the text display.
            updateTextDisplay()
        }

    private val mHandler = Handler(Looper.getMainLooper())
    private var mUpdateTimeTask: UpdateTimeRunnable? = null
    private var isUpdateTaskRunning = false

    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context, attrs: AttributeSet?, defStyle: Int) : super(ctx, attrs, defStyle)

    private fun updateTextDisplay() {
        if (referenceTime < 0) {
            setText(R.string.never)
        } else {
            text = getRelativeTimeDisplayString(referenceTime, System.currentTimeMillis())
        }
    }

    /**
     * Get the text to display for relative time.
     *
     * You can override this method to customize the string returned. For example you could add prefixes or suffixes, or use Spans to style the string etc
     * @param referenceTime The reference time passed in through [.setReferenceTime] or through `reference_time` attribute
     * @param now The current time
     * @return The display text for the relative time
     */
    open fun getRelativeTimeDisplayString(referenceTime: Long, now: Long): CharSequence {
        var difference: Long = abs(now - referenceTime)
        val days = difference / DateUtils.DAY_IN_MILLIS
        difference -= days * DateUtils.DAY_IN_MILLIS
        val hours = difference / DateUtils.HOUR_IN_MILLIS
        difference -= hours * DateUtils.HOUR_IN_MILLIS
        val minutes = difference / DateUtils.MINUTE_IN_MILLIS
        return if (referenceTime > now) {
            when {
                days > 0 -> context.getString(R.string.time_in_dhm, days, hours, minutes)
                hours > 0 -> context.getString(R.string.time_in_hm, hours, minutes)
                minutes > 0 -> context.getString(R.string.time_in_m, minutes)
                else -> context.getString(R.string.just_now)
            }
        } else {
            when {
                days > 0 -> context.getString(R.string.time_ago_dhm, days, hours, minutes)
                hours > 0 -> context.getString(R.string.time_ago_hm, hours, minutes)
                minutes > 0 -> context.getString(R.string.time_ago_m, minutes)
                else -> context.getString(R.string.just_now)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startTaskForPeriodicallyUpdatingRelativeTime()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopTaskForPeriodicallyUpdatingRelativeTime()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == GONE || visibility == INVISIBLE) {
            stopTaskForPeriodicallyUpdatingRelativeTime()
        } else {
            startTaskForPeriodicallyUpdatingRelativeTime()
        }
    }

    private fun startTaskForPeriodicallyUpdatingRelativeTime() {
        if (mUpdateTimeTask == null || mUpdateTimeTask!!.isDetached) initUpdateTimeTask()
        mHandler.post(mUpdateTimeTask!!)
        isUpdateTaskRunning = true
    }

    private fun initUpdateTimeTask() {
        mUpdateTimeTask = UpdateTimeRunnable(this, referenceTime)
    }

    private fun stopTaskForPeriodicallyUpdatingRelativeTime() {
        if (isUpdateTaskRunning) {
            mUpdateTimeTask!!.detach()
            mHandler.removeCallbacks(mUpdateTimeTask!!)
            isUpdateTaskRunning = false
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.referenceTime = referenceTime
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        referenceTime = state.referenceTime
        super.onRestoreInstanceState(state.superState)
    }

    private class SavedState : BaseSavedState {
        var referenceTime: Long = 0

        constructor(superState: Parcelable?) : super(superState)
        constructor(`in`: Parcel) : super(`in`) {
            referenceTime = `in`.readLong()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeLong(referenceTime)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    private class UpdateTimeRunnable(view: BetterRelativeTimeTextView?, private val mRefTime: Long) : Runnable
    {
        private val viewWeakRef: WeakReference<BetterRelativeTimeTextView?> = WeakReference(view)

        val isDetached: Boolean
            get() = viewWeakRef.get() == null

        fun detach() {
            viewWeakRef.clear()
        }

        override fun run() {
            val view = viewWeakRef.get() ?: return
            val difference = abs(System.currentTimeMillis() - mRefTime)
            val differenceRoundedToMinute = (difference/DateUtils.MINUTE_IN_MILLIS)*DateUtils.MINUTE_IN_MILLIS
            view.updateTextDisplay()
            view.mHandler.postDelayed(this, difference-differenceRoundedToMinute)
        }
    }
}
