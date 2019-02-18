package tanzent.cassette.appwidgets.small

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import tanzent.cassette.R
import tanzent.cassette.appwidgets.AppWidgetSkin
import tanzent.cassette.appwidgets.BaseAppwidget
import tanzent.cassette.service.MusicService

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/12/28 11:22
 */

class AppWidgetSmall : BaseAppwidget() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        defaultAppWidget(context, appWidgetIds)
        val intent = Intent(MusicService.ACTION_WIDGET_UPDATE)
        intent.putExtra("WidgetName", "SmallWidget")
        intent.putExtra("WidgetIds", appWidgetIds)
        intent.flags = Intent.FLAG_RECEIVER_REGISTERED_ONLY
        context.sendBroadcast(intent)
    }

    private fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
        val remoteViews = RemoteViews(context.packageName, R.layout.app_widget_small)
        buildAction(context, remoteViews)
        pushUpdate(context, appWidgetIds, remoteViews)
    }

    override fun updateWidget(service: MusicService, appWidgetIds: IntArray?, reloadCover: Boolean) {
        val song = service.currentSong
        if (song == null || !hasInstances(service))
            return
        val remoteViews = RemoteViews(service.packageName, R.layout.app_widget_small)
        buildAction(service, remoteViews)
        mSkin = AppWidgetSkin.WHITE_1F
        updateRemoteViews(service, remoteViews, song)
        //设置封面
        updateCover(service, remoteViews, appWidgetIds, reloadCover)
    }
}
