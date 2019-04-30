package tanzent.cassette.misc.tageditor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import tanzent.cassette.bean.mp3.Song
import tanzent.cassette.misc.interfaces.OnTagEditListener

class TagReceiver(private val listener: OnTagEditListener?) : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val newSong = intent.getParcelableExtra<Song>("newSong")
    listener?.onTagEdit(newSong)
  }

  companion object {
    const val ACTION_EDIT_TAG = "tanzent.music.ACTION_EDIT_TAG"
  }
}
