package tanzent.cassette.ui.widget

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import tanzent.cassette.R
import tanzent.cassette.theme.ThemeStore
import tanzent.cassette.theme.TintHelper
import tanzent.cassette.util.StatusBarUtil

class MultiPopupWindow(activity: Activity) : PopupWindow(activity) {

  init {
    contentView = LayoutInflater.from(activity).inflate(R.layout.toolbar_multi, activity.window.decorView as ViewGroup, false)
    width = ViewGroup.LayoutParams.MATCH_PARENT
    val ta = activity.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
    val actionBarSize = ta.getDimensionPixelSize(0, 0)
    ta.recycle()
    height = actionBarSize
    setBackgroundDrawable(ColorDrawable(ThemeStore.getMaterialPrimaryColor()))
    isFocusable = false
    isOutsideTouchable = false


    TintHelper.setTintAuto(contentView.findViewById(R.id.multi_close),
//                if (ThemeStore.isMDColorCloseToWhite()) Color.BLACK else Color.WHITE,
        ThemeStore.getMaterialPrimaryColorReverse(),
        false)
  }

  fun show(parent: View) {
    showAsDropDown(parent, 0, 0)
  }
}