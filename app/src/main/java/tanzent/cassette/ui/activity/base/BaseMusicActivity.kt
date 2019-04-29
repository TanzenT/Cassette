package tanzent.cassette.ui.activity.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.text.TextUtils
import com.facebook.drawee.backends.pipeline.Fresco
import com.soundcloud.android.crop.Crop
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import tanzent.cassette.R
import tanzent.cassette.bean.misc.CustomCover
import tanzent.cassette.helper.MusicEventCallback
import tanzent.cassette.helper.MusicServiceRemote
import tanzent.cassette.misc.cache.DiskCache
import tanzent.cassette.request.SimpleUriRequest
import tanzent.cassette.request.network.RxUtil
import tanzent.cassette.service.MusicService
import tanzent.cassette.util.Constants
import tanzent.cassette.util.ImageUriUtil.getSearchRequestWithAlbumType
import tanzent.cassette.util.MediaStoreUtil
import tanzent.cassette.util.ToastUtil
import tanzent.cassette.util.Util
import tanzent.cassette.util.Util.registerLocalReceiver
import tanzent.cassette.util.Util.unregisterLocalReceiver
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

@SuppressLint("Registered")
open class BaseMusicActivity : BaseActivity(), MusicEventCallback {
  private var serviceToken: MusicServiceRemote.ServiceToken? = null
  private val serviceEventListeners = ArrayList<MusicEventCallback>()
  private var musicStateReceiver: MusicStateReceiver? = null
  private var receiverRegistered: Boolean = false
  private var pendingBindService = false

  private val TAG = this.javaClass.simpleName

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Timber.tag(TAG).v("onCreate")
    bindToService()
  }

  override fun onStart() {
    super.onStart()
    Timber.tag(TAG).v("onStart(), $pendingBindService")
//    if (pendingBindService) {
//      bindToService()
//    }
  }

  override fun onRestart() {
    super.onRestart()
    Timber.tag(TAG).v("onRestart")
  }

  override fun onResume() {
    super.onResume()
    Timber.tag(TAG).v("onResume")
    if (pendingBindService) {
      bindToService()
    }
  }

  override fun onPause() {
    super.onPause()
    Timber.tag(TAG).v("onPause")
  }

  override fun onDestroy() {
    super.onDestroy()
    Timber.tag(TAG).v("onDestroy")
    MusicServiceRemote.unbindFromService(serviceToken)
    mMusicStateHandler?.removeCallbacksAndMessages(null)
    if (receiverRegistered) {
      unregisterLocalReceiver(musicStateReceiver)
      receiverRegistered = true
    }
  }

  private fun bindToService() {
    if (!Util.isAppOnForeground()) {
      Timber.tag(TAG).v("bindToService(),app isn't on foreground")
      pendingBindService = true
      return
    }
    serviceToken = MusicServiceRemote.bindToService(this, object : ServiceConnection {
      override fun onServiceConnected(name: ComponentName, service: IBinder) {
        val musicService = (service as MusicService.MusicBinder).service
        this@BaseMusicActivity.onServiceConnected(musicService)
      }

      override fun onServiceDisconnected(name: ComponentName) {
        this@BaseMusicActivity.onServiceDisConnected()
      }
    })
    pendingBindService = false
  }

  fun addMusicServiceEventListener(listener: MusicEventCallback?) {
    if (listener != null) {
      serviceEventListeners.add(listener)
    }
  }

  fun removeMusicServiceEventListener(listener: MusicEventCallback?) {
    if (listener != null) {
      serviceEventListeners.remove(listener)
    }
  }

  override fun onMediaStoreChanged() {
    Timber.tag(TAG).v("onMediaStoreChanged")
    for (listener in serviceEventListeners) {
      listener.onMediaStoreChanged()
    }
  }

  override fun onPermissionChanged(has: Boolean) {
    Timber.tag(TAG).v("onPermissionChanged(), $has")
    mHasPermission = has
    for (listener in serviceEventListeners) {
      listener.onPermissionChanged(has)
    }
  }

  override fun onPlayListChanged(name: String) {
    Timber.tag(TAG).v("onMediaStoreChanged(), $name")
    for (listener in serviceEventListeners) {
      listener.onPlayListChanged(name)
    }
  }

  override fun onMetaChanged() {
    Timber.tag(TAG).v("onMetaChange")
    for (listener in serviceEventListeners) {
      listener.onMetaChanged()
    }
  }

  override fun onPlayStateChange() {
    Timber.tag(TAG).v("onPlayStateChange")
    for (listener in serviceEventListeners) {
      listener.onPlayStateChange()
    }
  }

  override fun onServiceConnected(service: MusicService) {
    Timber.tag(TAG).v("onServiceConnected(), $service")
    if (!receiverRegistered) {
      musicStateReceiver = MusicStateReceiver(this)
      val filter = IntentFilter()
      filter.addAction(MusicService.PLAYLIST_CHANGE)
      filter.addAction(MusicService.PERMISSION_CHANGE)
      filter.addAction(MusicService.MEDIA_STORE_CHANGE)
      filter.addAction(MusicService.META_CHANGE)
      filter.addAction(MusicService.PLAY_STATE_CHANGE)
      registerLocalReceiver(musicStateReceiver, filter)
      receiverRegistered = true
    }
    for (listener in serviceEventListeners) {
      listener.onServiceConnected(service)
    }
    mMusicStateHandler = MusicStateHandler(this)
  }

  override fun onServiceDisConnected() {
    if (receiverRegistered) {
      unregisterLocalReceiver(musicStateReceiver)
      receiverRegistered = false
    }
    for (listener in serviceEventListeners) {
      listener.onServiceDisConnected()
    }
    mMusicStateHandler?.removeCallbacksAndMessages(null)
  }

  private var mMusicStateHandler: MusicStateHandler? = null

  private class MusicStateHandler(activity: BaseMusicActivity) : Handler() {
    private val mRef: WeakReference<BaseMusicActivity> = WeakReference(activity)

    override fun handleMessage(msg: Message?) {
      val action = msg?.obj?.toString()
      val activity = mRef.get()
      if (action != null && activity != null) {
        when (action) {
          MusicService.MEDIA_STORE_CHANGE -> {
            activity.onMediaStoreChanged()
          }
          MusicService.PERMISSION_CHANGE -> {
            activity.onPermissionChanged(msg.data.getBoolean(EXTRA_PERMISSION))
          }
          MusicService.PLAYLIST_CHANGE -> {
            activity.onPlayListChanged(msg.data.getString(EXTRA_PLAYLIST))
          }
          MusicService.META_CHANGE -> {
            activity.onMetaChanged()
          }
          MusicService.PLAY_STATE_CHANGE -> {
            activity.onPlayStateChange()
          }
        }
      }

    }
  }

  private class MusicStateReceiver(activity: BaseMusicActivity) : BroadcastReceiver() {
    private val mRef: WeakReference<BaseMusicActivity> = WeakReference(activity)

    override fun onReceive(context: Context, intent: Intent) {
      mRef.get()?.mMusicStateHandler?.let {
        val action = intent.action
        val msg = it.obtainMessage(action.hashCode())
        msg.obj = action
        msg.data.putString(EXTRA_PLAYLIST, intent.getStringExtra(EXTRA_PLAYLIST))
        msg.data.putBoolean(EXTRA_PERMISSION, intent.getBooleanExtra(EXTRA_PERMISSION, false))
        it.removeMessages(msg.what)
        it.sendMessageDelayed(msg, 200)
      }
//            if (activity != null && action != null) {
//                when (action) {
//                    MusicService.MEDIA_STORE_CHANGE -> {
//                        activity.onMediaStoreChanged()
//                    }
//                    MusicService.PERMISSION_CHANGE -> {
//                        activity.onPermissionChanged(intent.getBooleanExtra("permission", false))
//                    }
//                    MusicService.PLAYLIST_CHANGE -> {
//                        activity.onPlayListChanged()
//                    }
//                    MusicService.META_CHANGE ->{
//                        activity.onMetaChanged()
//                    }
//                    MusicService.PLAY_STATE_CHANGE ->{
//                        activity.onPlayStateChange()
//                    }
//                }
//            }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      Crop.REQUEST_CROP, Crop.REQUEST_PICK -> {
        val intent = intent
        val customCover = intent.getParcelableExtra<CustomCover>("thumb") ?: return
        val errorTxt = getString(
            when {
              customCover.type == Constants.ALBUM -> R.string.set_album_cover_error
              customCover.type == Constants.ARTIST -> R.string.set_artist_cover_error
              else -> R.string.set_playlist_cover_error
            })
        val id = customCover.id //专辑、艺术家、播放列表封面

        if (resultCode != Activity.RESULT_OK) {
          ToastUtil.show(this, errorTxt)
          return
        }
        if (requestCode == Crop.REQUEST_PICK) {
          //选择图片
          val cacheDir = DiskCache.getDiskCacheDir(this,
              "thumbnail/" + when {
                customCover.type == Constants.ALBUM -> "album"
                customCover.type == Constants.ARTIST -> "artist"
                else -> "playlist"
              })
          if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            ToastUtil.show(this, errorTxt)
            return
          }
          val destination = Uri.fromFile(File(cacheDir, Util.hashKeyForDisk(id.toString() + "")))
          Crop.of(data?.data, destination).asSquare().start(this)
        } else {
          //图片裁剪
          //裁剪后的图片路径
          if (data == null) {
            return
          }
          if (Crop.getOutput(data) == null) {
            return
          }

          val path = Crop.getOutput(data).encodedPath
          if (TextUtils.isEmpty(path) || id == -1) {
            ToastUtil.show(mContext, errorTxt)
            return
          }
          Observable
              .create(ObservableOnSubscribe<Uri> { emitter ->
                //获取以前的图片
                if (customCover.type == Constants.ALBUM) {
                  object : SimpleUriRequest(getSearchRequestWithAlbumType(
                      MediaStoreUtil.getSongByAlbumId(customCover.id))) {
                    override fun onError(throwable: Throwable) {
                      emitter.onError(throwable)
                    }

                    override fun onSuccess(result: Uri?) {
                      emitter.onNext(result!!)
                      emitter.onComplete()
                    }
                  }.load()
                } else {
                  emitter.onNext(Uri.parse("file://$path"))
                  emitter.onComplete()
                }
              })
              .doOnSubscribe {
                //如果设置的是专辑封面 修改内嵌封面
                if (customCover.type == Constants.ALBUM) {
                  MediaStoreUtil.saveArtwork(mContext, customCover.id, File(path))
                }
              }
              .compose(RxUtil.applyScheduler())
              .doFinally {
                onMediaStoreChanged()
              }
              .subscribe({ uri ->
                val imagePipeline = Fresco.getImagePipeline()
                imagePipeline.evictFromCache(uri)
                imagePipeline.evictFromDiskCache(uri)
              }, { throwable -> ToastUtil.show(mContext, R.string.save_error, throwable.toString()) })
        }
      }
    }
  }

  companion object {
    const val EXTRA_PLAYLIST = "extra_playlist"
    const val EXTRA_PERMISSION = "extra_permission"


    //更新适配器
    const val UPDATE_ADAPTER = 100
    //多选更新
    const val CLEAR_MULTI = 101
    //重建activity
    const val RECREATE_ACTIVITY = 102
  }
}
