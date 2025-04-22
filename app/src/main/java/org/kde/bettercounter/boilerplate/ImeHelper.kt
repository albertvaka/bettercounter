package org.kde.bettercounter.boilerplate

import android.content.Context.INPUT_METHOD_SERVICE
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun isKeyboardVisible(rootView: View): Boolean {
    val imeInsets = ViewCompat.getRootWindowInsets(rootView)?.getInsets(WindowInsetsCompat.Type.ime()) ?: return false
    return imeInsets.bottom > 0
}

fun hideKeyboard(rootView: View) {
    val imm = rootView.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(rootView.windowToken, 0)
}
