package tanzent.cassette.misc.menu

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.PopupMenu
import android.view.MenuItem
import com.afollestad.materialdialogs.DialogAction.POSITIVE
import com.soundcloud.android.crop.Crop
import tanzent.cassette.App
import tanzent.cassette.R
import tanzent.cassette.bean.misc.CustomCover
import tanzent.cassette.bean.mp3.Song
import tanzent.cassette.db.room.DatabaseRepository
import tanzent.cassette.helper.DeleteHelper
import tanzent.cassette.request.network.RxUtil.applySingleScheduler
import tanzent.cassette.service.Command
import tanzent.cassette.service.MusicService.Companion.EXTRA_SONG
import tanzent.cassette.theme.Theme
import tanzent.cassette.ui.misc.Tag
import tanzent.cassette.ui.dialog.AddtoPlayListDialog
import tanzent.cassette.util.*
import tanzent.cassette.util.SPUtil.SETTING_KEY
import java.lang.ref.WeakReference
import kotlin.coroutines.experimental.coroutineContext

/**
 * Created by Remix on 2018/3/5.
 */

class SongPopupListener(activity: AppCompatActivity,
                        private val song: Song,
                        private val isDeletePlayList: Boolean,
                        private val playListName: String) : PopupMenu.OnMenuItemClickListener {
  private val tag = Tag(activity, song)
  private val ref = WeakReference(activity)

  override fun onMenuItemClick(item: MenuItem): Boolean {
    val activity = ref.get() ?: return true

    when (item.itemId) {
      R.id.menu_next -> {
        Util.sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.ADD_TO_NEXT_SONG)
            .putExtra(EXTRA_SONG, song))
      }
      R.id.menu_add_to_playlist -> {
        AddtoPlayListDialog.newInstance(listOf(song.id))
            .show(activity.supportFragmentManager, AddtoPlayListDialog::class.java.simpleName)
      }
      R.id.menu_add_to_play_queue -> {
        DatabaseRepository.getInstance()
            .insertToPlayQueue(listOf(song.id))
            .compose(applySingleScheduler())
            .subscribe { it -> ToastUtil.show(activity, activity.getString(R.string.add_song_playqueue_success, it)) }
      }
      R.id.menu_detail -> {
        tag.detail()
      }
      R.id.menu_edit -> {
        tag.edit()
      }
      R.id.menu_album_thumb -> {
        val customCover = CustomCover(song.albumId, Constants.ALBUM,
            song.album)
        val coverIntent = activity.intent
        coverIntent.putExtra("thumb", customCover)
        activity.intent = coverIntent
        Crop.pickImage(activity, Crop.REQUEST_PICK)
      }
      R.id.menu_ring -> {
        MediaStoreUtil.setRing(activity, song.id)
      }
      R.id.menu_share -> {
        activity.startActivity(
            Intent.createChooser(Util.createShareSongFileIntent(song, activity), null))
      }
      R.id.menu_delete -> {
        val title = activity.getString(R.string.confirm_delete_from_playlist_or_library,
            if (isDeletePlayList) playListName else activity.getString(R.string.library))
        Theme.getBaseDialog(activity)
            .content(title)
            .positiveText(R.string.confirm)
            .negativeText(R.string.cancel)
            .checkBoxPromptRes(R.string.delete_source, SPUtil
                .getValue(App.getContext(), SETTING_KEY.NAME,
                    SETTING_KEY.DELETE_SOURCE, false), null)
            .onAny { dialog, which ->
              if (which == POSITIVE) {
                DeleteHelper
                    .deleteSong(song.id, dialog.isPromptCheckBoxChecked, isDeletePlayList, playListName)
                    .subscribe({ success -> ToastUtil.show(activity, if (success) R.string.delete_success else R.string.delete_error) }, { ToastUtil.show(activity, R.string.delete_error) })
                activity.startActivity(
                        Intent.createChooser(Util.refreshAdapterAndLibrary(activity), null)
                )
              }
            }
            .show()
      }
    }
    return true
  }
}
