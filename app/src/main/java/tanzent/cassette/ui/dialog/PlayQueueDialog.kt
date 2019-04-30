package tanzent.cassette.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import butterknife.BindView
import butterknife.ButterKnife
import tanzent.cassette.R
import tanzent.cassette.bean.mp3.Song
import tanzent.cassette.db.room.DatabaseRepository
import tanzent.cassette.db.room.model.PlayQueue
import tanzent.cassette.helper.MusicServiceRemote
import tanzent.cassette.misc.asynctask.WrappedAsyncTaskLoader
import tanzent.cassette.misc.interfaces.OnItemClickListener
import tanzent.cassette.service.Command
import tanzent.cassette.service.MusicService.Companion.EXTRA_POSITION
import tanzent.cassette.theme.Theme
import tanzent.cassette.ui.adapter.PlayQueueAdapter
import tanzent.cassette.ui.dialog.base.BaseMusicDialog
import tanzent.cassette.ui.widget.fastcroll_recyclerview.LocationRecyclerView
import tanzent.cassette.util.DensityUtil
import tanzent.cassette.util.MusicUtil.makeCmdIntent
import tanzent.cassette.util.Util.sendLocalBroadcast
import timber.log.Timber

/**
 * Created by Remix on 2015/12/6.
 */

/**
 * 正在播放列表Dialog
 */
class PlayQueueDialog : BaseMusicDialog(), LoaderManager.LoaderCallbacks<List<Song>> {
  @BindView(R.id.playqueue_recyclerview)
  lateinit var recyclerView: LocationRecyclerView

  val adapter: PlayQueueAdapter by lazy {
    PlayQueueAdapter(requireContext(), R.layout.item_playqueue)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = Theme.getBaseDialog(activity)
        .customView(R.layout.dialog_playqueue, false)
        .build()

    ButterKnife.bind(this, dialog)

    recyclerView.adapter = adapter
    recyclerView.layoutManager = LinearLayoutManager(context)
    recyclerView.itemAnimator = DefaultItemAnimator()


    adapter.setOnItemClickListener(object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        sendLocalBroadcast(makeCmdIntent(Command.PLAYSELECTEDSONG)
            .putExtra(EXTRA_POSITION, position))
      }

      override fun onItemLongClick(view: View, position: Int) {}
    })

    //改变播放列表高度，并置于底部
    val window = dialog.window
    window!!.setWindowAnimations(R.style.DialogAnimBottom)
    val display = requireActivity().windowManager.defaultDisplay
    val metrics = DisplayMetrics()
    display.getMetrics(metrics)
    val lp = window.attributes
    lp.height = DensityUtil.dip2px(context, 354f)
    lp.width = metrics.widthPixels
    window.attributes = lp
    window.setGravity(Gravity.BOTTOM)

    //初始化LoaderManager
    loaderManager.initLoader(LOADER_ID++, null, this)

    onViewCreated(dialog.customView!!, savedInstanceState)
    return dialog

  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<Song>> {
    return AsyncPlayQueueSongLoader(requireContext())
  }

  override fun onLoadFinished(loader: Loader<List<Song>>, data: List<Song>?) {
    if (data == null) {
      return
    }
    adapter.setData(data)
    val currentId = MusicServiceRemote.getCurrentSong().id
    if (currentId < 0) {
      return
    }
    recyclerView.smoothScrollToCurrentSong(data)
  }

  override fun onLoaderReset(loader: Loader<List<Song>>) {
    adapter.setData(null)
  }


  override fun onMetaChanged() {
    super.onMetaChanged()
    adapter.notifyDataSetChanged()
  }

  override fun onPlayListChanged(name: String) {
    super.onPlayListChanged(name)
    if (name == PlayQueue.TABLE_NAME) {
      if (hasPermission) {
        loaderManager.restartLoader(LOADER_ID, null, this)
      } else {
        adapter.setData(null)
      }
    }
  }


  class AsyncPlayQueueSongLoader constructor(context: Context) : WrappedAsyncTaskLoader<List<Song>>(context) {

    override fun loadInBackground(): List<Song>? {
      return DatabaseRepository.getInstance()
          .getPlayQueueSongs()
          .onErrorReturn { throwable ->
            Timber.v(throwable)
            emptyList()
          }
          .blockingGet()
    }

  }

  companion object {

    @JvmStatic
    fun newInstance(): PlayQueueDialog {
      val playQueueDialog = PlayQueueDialog()
      return playQueueDialog
    }

    private var LOADER_ID = 0
  }

}
