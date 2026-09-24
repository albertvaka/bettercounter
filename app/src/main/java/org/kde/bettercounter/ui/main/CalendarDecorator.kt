package org.kde.bettercounter.ui.main
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Parcel
import android.os.Parcelable
import androidx.core.content.ContextCompat
import com.google.android.material.datepicker.DayViewDecorator
import org.kde.bettercounter.R
import java.time.LocalDate

class CalendarDecorator(private val dates: Set<LocalDate>) : DayViewDecorator() {

    private lateinit var normalCircle: Drawable
    private lateinit var selectedCircle: Drawable

    override fun initialize(context: Context) {
        val size = (4 * context.resources.displayMetrics.density).toInt()
        val offsetY = (4 * context.resources.displayMetrics.density).toInt()

        fun circle(color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setSize(size, size)
            setBounds(0, -offsetY, size, size - offsetY)
        }

        normalCircle = circle(ContextCompat.getColor(context, R.color.colorAccent))
        selectedCircle = circle(ContextCompat.getColor(context, R.color.colorDarkBackground))
    }

    override fun getCompoundDrawableBottom(
        context: Context,
        year: Int,
        month: Int,
        day: Int,
        valid: Boolean,
        selected: Boolean,
    ): Drawable? {
        if (!valid) {
            return null
        }
        if (LocalDate.of(year, month + 1, day) !in dates) {
            return null
        }
        return if (selected) selectedCircle else normalCircle
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(dates.size)
        for (date in dates) {
            parcel.writeInt(date.year)
            parcel.writeInt(date.monthValue)
            parcel.writeInt(date.dayOfMonth)
        }
    }

    companion object CREATOR : Parcelable.Creator<CalendarDecorator> {
        override fun createFromParcel(parcel: Parcel): CalendarDecorator {
            val count = parcel.readInt()
            val dates = buildSet {
                repeat(count) {
                    val year = parcel.readInt()
                    val month = parcel.readInt()
                    val day = parcel.readInt()
                    add(LocalDate.of(year, month, day))
                }
            }
            return CalendarDecorator(dates)
        }

        override fun newArray(size: Int): Array<CalendarDecorator?> = arrayOfNulls(size)
    }
}
