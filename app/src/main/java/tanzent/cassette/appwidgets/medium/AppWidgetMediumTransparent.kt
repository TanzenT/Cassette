package tanzent.cassette.appwidgets.medium

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import tanzent.cassette.R
import tanzent.cassette.appwidgets.AppWidgetSkin
import tanzent.cassette.appwidgets.BaseAppwidget
import tanzent.cassette.bean.mp3.Song
import tanzent.cassette.service.MusicService
import tanzent.cassette.util.Util

class AppWidgetMediumTransparent : BaseAppwidget() {
  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    super.onUpdate(context,appWidgetManager,appWidgetIds)
    defaultAppWidget(context, appWidgetIds)
    val intent = Intent(MusicService.ACTION_WIDGET_UPDATE)
    intent.putExtra(EXTRA_WIDGET_NAME, this.javaClass.simpleName)
    intent.putExtra(EXTRA_WIDGET_IDS, appWidgetIds)
    intent.flags = Intent.FLAG_RECEIVER_REGISTERED_ONLY
    context.sendBroadcast(intent)
  }

  private fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
    val remoteViews = RemoteViews(context.packageName, R.layout.app_widget_medium_transparent)
    buildAction(context, remoteViews)
    pushUpdate(context, appWidgetIds, remoteViews)
  }


  override fun updateWidget(service: MusicService, appWidgetIds: IntArray?, reloadCover: Boolean) {
    val song = service.currentSong
    if(song == Song.EMPTY_SONG){
      return
    }
    if(!hasInstances(service)){
      return
    }
    val remoteViews = RemoteViews(service.packageName, R.layout.app_widget_medium_transparent)
    buildAction(service, remoteViews)
    skin = AppWidgetSkin.TRANSPARENT
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

  override fun partiallyUpdateWidget(service: MusicService) {
    val song = service.currentSong
    if(song == Song.EMPTY_SONG){
      return
    }
    if(!hasInstances(service)){
      return
    }
    val remoteViews = RemoteViews(service.packageName, R.layout.app_widget_medium_transparent)
    buildAction(service, remoteViews)
    skin = AppWidgetSkin.TRANSPARENT
    updateRemoteViews(service, remoteViews, song)
    //设置时间
    val currentTime = service.progress.toLong()
    val remainTime = song.duration - service.progress
    if (currentTime > 0 && remainTime > 0) {
      remoteViews.setTextViewText(R.id.appwidget_progress, Util.getTime(currentTime) + "/" + Util.getTime(remainTime))
    }

    val appIds = AppWidgetManager.getInstance(service).getAppWidgetIds(ComponentName(service, javaClass))
    pushPartiallyUpdate(service,appIds,remoteViews)
  }

  companion object {
    @Volatile
    private var INSTANCE: AppWidgetMediumTransparent? = null

    @JvmStatic
    fun getInstance(): AppWidgetMediumTransparent =
        INSTANCE ?: synchronized(this) {
          INSTANCE ?: AppWidgetMediumTransparent()
        }
  }
}

