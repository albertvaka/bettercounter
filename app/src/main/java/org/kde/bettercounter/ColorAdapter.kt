package org.kde.bettercounter
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView

// Based on https://github.com/kristiyanP/colorpicker
class ColorAdapter(context: Context) : RecyclerView.Adapter<ColorAdapter.ViewHolder>() {

    var selectedColor : Int
        get() {
            return colors[selectedPosition]
        }
        set(color) {
            for (i in colors.indices) {
                val colorPal = colors[i]
                if (colorPal == color) {
                    selectedPosition = i
                }
            }
        }

    private var selectedPosition = 0
        set(pos) {
            val prevSelected = selectedPosition
            field = pos
            notifyItemChanged(selectedPosition)
            notifyItemChanged(prevSelected)
        }

    private var colors : List<Int>

    init {
        val colorList = mutableListOf<Int>()
        val resArray = context.resources.obtainTypedArray(R.array.picker_colors)
        for (i in 0 until resArray.length()) {
            colorList.add(resArray.getColor(i, 0))
        }
        resArray.recycle()
        colors = colorList
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        var colorButton: AppCompatButton = v as AppCompatButton
        init {
            colorButton.setOnClickListener(this)
        }
        override fun onClick(v: View) {
            selectedPosition = layoutPosition
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.color_circle, parent, false)
        return ViewHolder(view)
    }

    private fun isDarkColor(@ColorInt color: Int): Boolean {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue: Int = Color.blue(color)

        // https://en.wikipedia.org/wiki/YIQ
        // https://24ways.org/2010/calculating-color-contrast/
        val yiq: Int = ((red * 299) + (green * 587) + (blue * 114)) / 1000
        return yiq < 192
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val color = colors[position]
        val textColor = if (isDarkColor(color)) Color.WHITE else Color.BLACK
        val tickText = if (position == selectedPosition) "✔" else ""

        holder.colorButton.text = tickText
        holder.colorButton.setTextColor(textColor)
        holder.colorButton.background.setTint(color)
    }

    override fun getItemCount(): Int {
        return colors.size
    }

}