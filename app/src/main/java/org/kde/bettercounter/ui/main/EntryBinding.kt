package org.kde.bettercounter.ui.main

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import org.kde.bettercounter.databinding.CompactFragmentEntryBinding
import org.kde.bettercounter.databinding.FragmentEntryBinding

class EntryBinding {
    val root: View
    val decreaseButton: ImageButton?
    val increaseButton: ImageButton
    val draggableArea: View
    val nameText: TextView
    val countText: TextView
    val timestampText: BetterRelativeTimeTextView

    constructor(b: CompactFragmentEntryBinding) {
        root = b.root;
        decreaseButton = null
        increaseButton = b.increaseButton;
        draggableArea = b.draggableArea
        nameText = b.nameText;
        countText = b.countText;
        timestampText = b.timestampText
    }

    constructor(b: FragmentEntryBinding) {
        root = b.root;
        decreaseButton = b.decreaseButton
        increaseButton = b.increaseButton;
        draggableArea = b.draggableArea
        nameText = b.nameText;
        countText = b.countText;
        timestampText = b.timestampText
    }
}
