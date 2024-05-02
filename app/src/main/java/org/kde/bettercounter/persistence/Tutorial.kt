package org.kde.bettercounter.persistence

import android.content.Context
import android.view.Gravity
import android.view.View
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import org.kde.bettercounter.R

enum class Tutorial {
    DRAG,
    PICK_DATE,
    CHANGE_GRAPH_INTERVAL;

    private fun getText() = when(this) {
        DRAG -> R.string.tutorial_drag
        PICK_DATE -> R.string.tutorial_pickdate
        CHANGE_GRAPH_INTERVAL -> R.string.tutorial_change_graph_interval
    }

    fun show(context: Context, anchorView : View, onDismissListener: SimpleTooltip.OnDismissListener? = null) {
        SimpleTooltip.Builder(context)
            .anchorView(anchorView)
            .text(getText())
            .gravity(Gravity.BOTTOM)
            .animated(true)
            .focusable(true) // modal requires focusable
            .modal(true)
            .onDismissListener(onDismissListener)
            .build()
            .show()
    }

}
