package org.kde.bettercounter.boilerplate

import android.content.Context
import android.text.format.DateUtils
import android.util.AttributeSet
import com.github.curioustechizen.ago.RelativeTimeTextView
import org.kde.bettercounter.R

internal class BetterRelativeTimeTextView : RelativeTimeTextView {
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, style: Int) : super(context, attrs, style)

    override fun getRelativeTimeDisplayString(referenceTime: Long, now: Long): CharSequence {
        val difference: Long = now - referenceTime
        if (difference < DateUtils.MINUTE_IN_MILLIS) {
            return resources.getString(R.string.just_now)
        }
        return DateUtils.getRelativeDateTimeString(
            context,
            referenceTime,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE or DateUtils.FORMAT_NUMERIC_DATE
        )
    }
}