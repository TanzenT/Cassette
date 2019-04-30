package tanzent.cassette.misc.menu

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.provider.MediaStore
import android.support.v7.widget.PopupMenu
import android.view.MenuItem
import com.afollestad.materialdialogs.DialogAction.POSITIVE
import com.soundcloud.android.crop.Crop
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import tanzent.cassette.R
import tanzent.cassette.bean.misc.CustomCover
import tanzent.cassette.db.room.DatabaseRepository
import tanzent.cassette.helper.DeleteHelper
import tanzent.cassette.helper.MusicServiceRemote.setPlayQueue
import tanzent.cassette.request.network.RxUtil.applySingleScheduler
import tanzent.cassette.service.Command
import tanzent.cassette.service.MusicService.Companion.EXTRA_POSITION
import tanzent.cassette.theme.Theme
import tanzent.cassette.theme.ThemeStore
import tanzent.cassette.ui.activity.base.BaseActivity
import tanzent.cassette.ui.dialog.AddtoPlayListDialog
import tanzent.cassette.util.Constants
import tanzent.cassette.util.MediaStoreUtil.getSongIds
import tanzent.cassette.util.MediaStoreUtil.getSongIdsByParentId
import tanzent.cassette.util.MusicUtil.makeCmdIntent
import tanzent.cassette.util.SPUtil
import tanzent.cassette.util.ToastUtil

/**
 * Created by taeja on 16-1-25.
 */
class LibraryListener(private val context: Context, //专辑id 艺术家id 歌曲id 文件夹position
                      private val id: Int, //0:专辑 1:歌手 2:文件夹 3:播放列表
                      private val type: Int, //专辑名 艺术家名 文件夹position或者播放列表名字
                      private val key: String) : PopupMenu.OnMenuItemClickListener {

  private fun getSongIdSingle(): Single<List<Int>> {
    return Single.fromCallable {
      when (type) {
        //专辑或者艺术家
        Constants.ALBUM, Constants.ARTIST -> getSongIds((if (type == Constants.ALBUM) MediaStore.Audio.Media.ALBUM_ID else MediaStore.Audio.Media.ARTIST_ID) + "=" + id, null)
        //播放列表
        Constants.PLAYLIST -> DatabaseRepository.getInstance().getPlayList(id)
            .map {
              it.audioIds.toList()
            }
            .blockingGet()
        //文件夹
        Constants.FOLDER -> getSongIdsByParentId(id)
        else -> emptyList<Int>()
      }
    }
  }

  @SuppressLint("CheckResult")
  override fun onMenuItemClick(item: MenuItem): Boolean {
//        val ids = when (type) {
//            Constants.ALBUM, Constants.ARTIST //专辑或者艺术家
//            -> getSongIds((if (type == Constants.ALBUM) MediaStore.Audio.Media.ALBUM_ID else MediaStore.Audio.Media.ARTIST_ID) + "=" + id, null)
//            Constants.PLAYLIST //播放列表
//            -> PlayListUtil.getSongIds(id)
//            Constants.FOLDER //文件夹
//            -> getSongIdsByParentId(id)
//            else -> emptyList<Int>()
//        }

    getSongIdSingle()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribe(Consumer { songIds ->
          when (item.itemId) {
            //播放
            R.id.menu_play -> {
              if (songIds == null || songIds.isEmpty()) {
                ToastUtil.show(context, R.string.list_is_empty)
                return@Consumer
              }
              setPlayQueue(songIds, makeCmdIntent(Command.PLAYSELECTEDSONG)
                  .putExtra(EXTRA_POSITION, 0))
            }
            //添加到播放队列
            R.id.menu_add_to_play_queue -> {
              if (songIds == null || songIds.isEmpty()) {
                ToastUtil.show(context, R.string.list_is_empty)
                return@Consumer
              }
              DatabaseRepository.getInstance()
                  .insertToPlayQueue(songIds)
                  .compose(applySingleScheduler())
                  .subscribe(Consumer {
                    ToastUtil.show(context, context.getString(R.string.add_song_playqueue_success, it))
                  })
            }
            //添加到播放列表
            R.id.menu_add_to_playlist -> {
              AddtoPlayListDialog.newInstance(songIds)
                  .show((context as BaseActivity).supportFragmentManager, AddtoPlayListDialog::class.java.simpleName)
            }
            //删除
            R.id.menu_delete -> {
              R.string.my_favorite
              if (key == context.getString(R.string.my_favorite)) {
                //我的收藏不可删除
                ToastUtil.show(context, R.string.mylove_cant_edit)
                return@Consumer
              }
              Theme.getBaseDialog(context)
                  .content(if (type == Constants.PLAYLIST) R.string.confirm_delete_playlist else R.string.confirm_delete_from_library)
                  .positiveText(R.string.confirm)
                  .negativeText(R.string.cancel)
                  .checkBoxPromptRes(R.string.delete_source, SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE, false), null)
                  .onAny { dialog, which ->
                    if (which == POSITIVE) {
                      DeleteHelper.deleteSongs(songIds, dialog.isPromptCheckBoxChecked, id, type == Constants.PLAYLIST)
                          .compose(applySingleScheduler())
                          .subscribe({
                            ToastUtil.show(context, if (it) R.string.delete_success else R.string.delete_error)
                          }, {
                            ToastUtil.show(context, R.string.delete_error)
                          })
                    }
                  }
                  .show()
            }
            //设置封面
            R.id.menu_album_thumb -> {
              val thumbBean = CustomCover(id, type, key)
              val thumbIntent = (context as Activity).intent
              thumbIntent.putExtra("thumb", thumbBean)
              context.intent = thumbIntent
              Crop.pickImage(context, Crop.REQUEST_PICK)
            }
            //列表重命名
            R.id.menu_playlist_rename -> {
              if (key == context.getString(R.string.my_favorite)) {
                //我的收藏不可删除
                ToastUtil.show(context, R.string.mylove_cant_edit)
                return@Consumer
              }
              Theme.getBaseDialog(context)
                  .title(R.string.rename)
                  .input("", "", false) { dialog, input ->
                    DatabaseRepository.getInstance()
                        .getPlayList(id)
                        .flatMap {
                          DatabaseRepository.getInstance()
                              .updatePlayList(it.copy(name = input.toString()))
                        }
                        .compose(applySingleScheduler())
                        .subscribe({
                          ToastUtil.show(context, R.string.save_success)
                        }, {
                          ToastUtil.show(context, R.string.save_error)
                        })
                  }
                  .buttonRippleColor(ThemeStore.getRippleColor())
                  .positiveText(R.string.confirm)
                  .negativeText(R.string.cancel)
                  .show()
            }
            else -> {
            }
          }
        })


    return true
  }

}
