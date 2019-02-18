package tanzent.cassette.appwidgets.medium

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import tanzent.cassette.R
import tanzent.cassette.appwidgets.AppWidgetSkin
import tanzent.cassette.appwidgets.BaseAppwidget
import tanzent.cassette.service.MusicService
import tanzent.cassette.util.Util

/**
 * Created by Remix on 2016/12/20.
 */

class AppWidgetMedium : BaseAppwidget() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        defaultAppWidget(context, appWidgetIds)
        val intent = Intent(MusicService.ACTION_WIDGET_UPDATE)
        intent.putExtra("WidgetName", "MediumWidget")
        intent.putExtra("WidgetIds", appWidgetIds)
        intent.flags = Intent.FLAG_RECEIVER_REGISTERED_ONLY
        context.sendBroadcast(intent)
    }

    private fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
        val remoteViews = RemoteViews(context.packageName, R.layout.app_widget_medium)
        buildAction(context, remoteViews)
        pushUpdate(context, appWidgetIds, remoteViews)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    override fun updateWidget(service: MusicService, appWidgetIds: IntArray?, reloadCover: Boolean) {
        val song = service.currentSong
        if (song == null || !hasInstances(service))
            return
        val remoteViews = RemoteViews(service.packageName, R.layout.app_widget_medium)
        buildAction(service, remoteViews)
        mSkin = AppWidgetSkin.WHITE_1F
        updateRemoteViews(service, remoteViews, song)
        //设置时间
        val currentTime = service.progress.toLong()
        val remainTime = song.duration - service.progress
        if (currentTime > 0 && remainTime > 0) {
            remoteViews.setTextViewText(R.id.appwidget_progress, Util.getTime(currentTime) + "/" + Util.getTime(remainTime))
        }
        //设置封面
        updateCover(service, remoteViews, appWidgetIds, reloadCover)
    }
}
