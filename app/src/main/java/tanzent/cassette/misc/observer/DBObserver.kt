package tanzent.cassette.misc.observer

import android.content.Intent
import android.net.Uri
import tanzent.cassette.Global
import tanzent.cassette.db.DBContentProvider
import tanzent.cassette.service.MusicService
import tanzent.cassette.util.PlayListUtil
import tanzent.cassette.util.Util.sendLocalBroadcast

/**
 * Created by Remix on 2016/10/19.
 */

class DBObserver
/**
 * Creates a content observer.
 *
 * @param service
 * @param handler The handler to run [.onChange] on, or null if none.
 */
(service: MusicService) : BaseObserver(service, null) {

    internal override fun onAccept(uri: Uri) {
        val match = DBContentProvider.getUriMatcher().match(uri)
        when (match) {
            //更新播放列表
            DBContentProvider.PLAY_LIST_MULTIPLE, DBContentProvider.PLAY_LIST_SINGLE -> Global.PlayList = PlayListUtil.getAllPlayListInfo()
            //更新播放队列
            DBContentProvider.PLAY_LIST_SONG_MULTIPLE, DBContentProvider.PLAY_LIST_SONG_SINGLE ->
                mService.get()?.playQueue = PlayListUtil.getSongIds(Global.PlayQueueID)
        }
        if (match != -1) {
            sendLocalBroadcast(Intent(MusicService.PLAYLIST_CHANGE))
        }
    }

    internal override fun onFilter(uri: Uri): Boolean {
        return true
    }
}
