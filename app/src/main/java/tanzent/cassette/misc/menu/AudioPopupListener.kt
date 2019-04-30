package tanzent.cassette.misc.menu

import android.content.ContextWrapper
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.support.v7.widget.PopupMenu
import android.view.MenuItem
import com.afollestad.materialdialogs.DialogAction.POSITIVE
import com.afollestad.materialdialogs.MaterialDialog
import tanzent.cassette.App
import tanzent.cassette.R
import tanzent.cassette.bean.mp3.Song
import tanzent.cassette.db.room.DatabaseRepository
import tanzent.cassette.helper.DeleteHelper
import tanzent.cassette.helper.MusicServiceRemote
import tanzent.cassette.helper.MusicServiceRemote.getCurrentSong
import tanzent.cassette.helper.MusicServiceRemote.getMediaPlayer
import tanzent.cassette.request.network.RxUtil.applySingleScheduler
import tanzent.cassette.service.Command
import tanzent.cassette.service.MusicService
import tanzent.cassette.theme.Theme.getBaseDialog
import tanzent.cassette.ui.misc.Tag
import tanzent.cassette.ui.activity.EQActivity
import tanzent.cassette.ui.activity.MainActivity
import tanzent.cassette.ui.activity.PlayerActivity
import tanzent.cassette.ui.dialog.AddtoPlayListDialog
import tanzent.cassette.ui.dialog.FileChooserDialog
import tanzent.cassette.ui.dialog.TimerDialog
import tanzent.cassette.util.SPUtil
import tanzent.cassette.util.ToastUtil
import tanzent.cassette.util.Util
import tanzent.cassette.util.Util.sendLocalBroadcast
import java.lang.ref.WeakReference

/**
 * @ClassName AudioPopupListener
 * @Description
 * @Author Xiaoborui
 * @Date 2016/8/29 15:33
 */
class AudioPopupListener<ActivityCallback>(activity: ActivityCallback, private val song: Song) : ContextWrapper(activity), PopupMenu.OnMenuItemClickListener
    where ActivityCallback : PlayerActivity, ActivityCallback : FileChooserDialog.FileCallback {
  private val tag: Tag = Tag(activity, song)
  private val ref = WeakReference(activity)

  override fun onMenuItemClick(item: MenuItem): Boolean {
    val activity = ref.get() ?: return true
    when (item.itemId) {
      R.id.menu_lyric -> {
        val alreadyIgnore = (SPUtil
            .getValue(ref.get(), SPUtil.LYRIC_KEY.NAME, song.id.toString(),
                SPUtil.LYRIC_KEY.LYRIC_DEFAULT) == SPUtil.LYRIC_KEY.LYRIC_IGNORE)

        val lyricFragment = ref.get()?.lyricFragment ?: return true
        getBaseDialog(ref.get())
            .items(getString(R.string.netease),
                getString(R.string.kugou),
                getString(R.string.local),
                getString(R.string.embedded_lyric),
                getString(R.string.select_lrc),
                getString(if (!alreadyIgnore) R.string.ignore_lrc else R.string.cancel_ignore_lrc),
                getString(R.string.change_offset))
            .itemsCallback { dialog, itemView, position, text ->
              when (position) {
                0, 1, 2, 3 -> { //0网易 1酷狗 2本地 3内嵌
                  SPUtil.putValue(ref.get(), SPUtil.LYRIC_KEY.NAME, song.id.toString(), position + 2)
                  lyricFragment.updateLrc(song, true)
                  sendLocalBroadcast(Intent(MusicService.ACTION_CMD)
                      .putExtra("Control", Command.CHANGE_LYRIC))
                }
                4 -> { //手动选择歌词
                  FileChooserDialog.Builder(activity)
                      .extensionsFilter(".lrc")
                      .show()
                }
                5 -> { //忽略或者取消忽略
                  getBaseDialog(activity)
                      .title(if (!alreadyIgnore) R.string.confirm_ignore_lrc else R.string.confirm_cancel_ignore_lrc)
                      .negativeText(R.string.cancel)
                      .positiveText(R.string.confirm)
                      .onPositive { dialog1, which ->
                        if (!alreadyIgnore) {//忽略
                          SPUtil.putValue(activity, SPUtil.LYRIC_KEY.NAME, song.id.toString(),
                              SPUtil.LYRIC_KEY.LYRIC_IGNORE)
                          lyricFragment.updateLrc(song)
                        } else {//取消忽略
                          SPUtil.putValue(activity, SPUtil.LYRIC_KEY.NAME, song.id.toString(),
                              SPUtil.LYRIC_KEY.LYRIC_DEFAULT)
                          lyricFragment.updateLrc(song)
                        }
                        sendLocalBroadcast(Intent(MusicService.ACTION_CMD)
                            .putExtra("Control", Command.CHANGE_LYRIC))
                      }
                      .show()
                }
                6 -> { //歌词时间轴调整
                  activity.showLyricOffsetView()
                }
              }

            }
            .show()
      }
      R.id.menu_edit -> {
        tag.edit()
      }
      R.id.menu_detail -> {
        tag.detail()
      }
      R.id.menu_timer -> {
        val fm = activity.supportFragmentManager ?: return true
        TimerDialog.newInstance().show(fm, TimerDialog::class.java.simpleName)
      }
      R.id.menu_eq -> {
        val audioEffectIntent = Intent(
            AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
        audioEffectIntent
            .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getMediaPlayer()!!.getAudioSessionId())
        if (Util.isIntentAvailable(activity, audioEffectIntent)) {
          activity.startActivityForResult(audioEffectIntent, 0)
        } else {
          activity.startActivity(Intent(activity, EQActivity::class.java))
        }
      }
      R.id.menu_collect -> {
        DatabaseRepository.getInstance()
            .insertToPlayList(listOf(song.id), getString(R.string.my_favorite))
            .compose<Int>(applySingleScheduler<Int>())
            .subscribe(
                { count -> ToastUtil.show(activity, getString(R.string.add_song_playlist_success, 1, getString(R.string.my_favorite))) },
                { throwable -> ToastUtil.show(activity, R.string.add_song_playlist_error) })
      }
      R.id.menu_add_to_playlist -> {
        AddtoPlayListDialog.newInstance(listOf(song.id))
            .show(activity.supportFragmentManager, AddtoPlayListDialog::class.java.simpleName)
      }
      R.id.menu_delete -> {
        getBaseDialog(activity)
            .content(R.string.confirm_delete_from_library)
            .positiveText(R.string.confirm)
            .negativeText(R.string.cancel)
            .checkBoxPromptRes(R.string.delete_source, SPUtil
                .getValue(App.getContext(), SPUtil.SETTING_KEY.NAME,
                    SPUtil.SETTING_KEY.DELETE_SOURCE, false), null)
            .onAny { dialog, which ->
              if (which == POSITIVE) {
                DeleteHelper.deleteSong(song.id, dialog.isPromptCheckBoxChecked, false, "")
                    .compose<Boolean>(applySingleScheduler<Boolean>())
                    .subscribe({ success ->
                      if (success!!) {
                        //移除的是正在播放的歌曲
                        if (song.id == getCurrentSong().id) {
                          Util.sendCMDLocalBroadcast(Command.NEXT)
                        }
                      }
                      ToastUtil.show(activity, if (success) R.string.delete_success else R.string.delete_error)
                        val intent = Intent(activity, MainActivity::class.java)
                        intent.action = Intent.ACTION_MAIN
                        intent.addCategory(Intent.CATEGORY_LAUNCHER)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }, { ToastUtil.show(activity, R.string.delete) })
              }
            }
            .show()
      }
      //            case R.id.menu_vol:
      //                AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
      //                if(audioManager != null){
      //                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
      //                }
      R.id.menu_speed -> {
        getBaseDialog(activity)
            .title(R.string.speed)
            .input(SPUtil.getValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SPEED, "1.0"),
                "",
                MaterialDialog.InputCallback { dialog, input ->
                  var speed = 0f
                  try {
                    speed = java.lang.Float.parseFloat(input.toString())
                  } catch (ignored: Exception) {

                  }

                  if (speed > 2.0f || speed < 0.1f) {
                    ToastUtil.show(App.getContext(), R.string.speed_range_tip)
                    return@InputCallback
                  }
                  MusicServiceRemote.setSpeed(speed)
                  SPUtil.putValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SPEED,
                      input.toString())
                })
            .show()
      }
    }
    return true
  }

}
