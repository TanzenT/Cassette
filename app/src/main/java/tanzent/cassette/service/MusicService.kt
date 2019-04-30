package tanzent.cassette.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.session.PlaybackState
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.support.annotation.WorkerThread
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import tanzent.cassette.R
import tanzent.cassette.appshortcuts.DynamicShortcutManager
import tanzent.cassette.appwidgets.BaseAppwidget
import tanzent.cassette.appwidgets.big.AppWidgetBig
import tanzent.cassette.appwidgets.medium.AppWidgetMedium
import tanzent.cassette.appwidgets.medium.AppWidgetMediumTransparent
import tanzent.cassette.appwidgets.small.AppWidgetSmall
import tanzent.cassette.appwidgets.small.AppWidgetSmallTransparent
import tanzent.cassette.bean.mp3.Song
import tanzent.cassette.bean.mp3.Song.EMPTY_SONG
import tanzent.cassette.db.room.DatabaseRepository
import tanzent.cassette.db.room.model.PlayQueue
import tanzent.cassette.helper.LanguageHelper
import tanzent.cassette.helper.MusicEventCallback
import tanzent.cassette.helper.ShakeDetector
import tanzent.cassette.helper.SleepTimer
import tanzent.cassette.lyric.LyricHolder
import tanzent.cassette.lyric.LyricHolder.Companion.LYRIC_FIND_INTERVAL
import tanzent.cassette.lyric.bean.LyricRowWrapper
import tanzent.cassette.misc.LogObserver
import tanzent.cassette.misc.Migration
import tanzent.cassette.misc.floatpermission.FloatWindowManager
import tanzent.cassette.misc.observer.MediaStoreObserver
import tanzent.cassette.misc.receiver.HeadsetPlugReceiver
import tanzent.cassette.misc.receiver.MediaButtonReceiver
import tanzent.cassette.request.RemoteUriRequest
import tanzent.cassette.request.RequestConfig
import tanzent.cassette.request.network.RxUtil.applySingleScheduler
import tanzent.cassette.service.notification.Notify
import tanzent.cassette.service.notification.NotifyImpl
import tanzent.cassette.service.notification.NotifyImpl24
import tanzent.cassette.ui.activity.LockScreenActivity
import tanzent.cassette.ui.activity.base.BaseActivity.EXTERNAL_STORAGE_PERMISSIONS
import tanzent.cassette.ui.activity.base.BaseMusicActivity.Companion.EXTRA_PERMISSION
import tanzent.cassette.ui.activity.base.BaseMusicActivity.Companion.EXTRA_PLAYLIST
import tanzent.cassette.ui.widget.desktop.DesktopLyricView
import tanzent.cassette.util.Constants.*
import tanzent.cassette.util.ImageUriUtil.getSearchRequestWithAlbumType
import tanzent.cassette.util.MediaStoreUtil
import tanzent.cassette.util.SPUtil
import tanzent.cassette.util.SPUtil.SETTING_KEY
import tanzent.cassette.util.ToastUtil
import tanzent.cassette.util.Util
import tanzent.cassette.util.Util.*
import timber.log.Timber
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by Remix on 2015/12/1.
 */

/**
 * 播放Service 歌曲的播放 控制 回调相关activity的界面更新 通知栏的控制
 */
@SuppressLint("CheckResult")
class MusicService : BaseService(), Playback, MusicEventCallback {
  /**
   * 所有歌曲id
   */
  val allSong: MutableList<Int> = ArrayList()
  /**
   * 播放队列id
   */
  val playQueue: MutableList<Int> = ArrayList()

  /**
   * 已经生成过的随机数 用于随机播放模式
   */
  private val randomQueue: MutableList<Int> = ArrayList()
  /**
   * 是否第一次准备完成
   */
  private var firstPrepared = true

  /**
   * 是否正在设置mediapplayer的datasource
   */
  private var prepared = false

  /**
   * 数据是否加载完成
   */
  private var loadFinished = false

  /**
   * 设置播放模式并更新下一首歌曲
   */
  var playModel: Int = PLAY_LOOP
    set(newPlayModel) {
      desktopWidgetTask?.run()
      SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_MODEL, newPlayModel)
//      SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.NEXT_SONG_ID, nextId)
//      SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_SONG_ID, currentId)
      if (newPlayModel == PLAY_SHUFFLE) {
        makeShuffleList(currentId)
      }
      field = newPlayModel
    }

  /**
   * 当前是否正在播放
   */
  private var isPlay: Boolean = false

  /**
   * 当前播放的索引
   */
  private var currentIndex = 0
  /**
   * 当前正在播放的歌曲id
   */
  private var currentId = -1

  /**
   * 返回当前播放歌曲
   */
  var currentSong: Song = Song.EMPTY_SONG

  /**
   * 当前播放的歌曲是否收藏
   */
  var isLove: Boolean = false

  /**
   * 下一首歌曲的索引
   */
  private var nextIndex = 0
  /**
   * 下一首播放歌曲的id
   */
  private var nextId = -1
  /**
   * 下一首播放的mp3
   */
  /**
   * 返回下一首播放歌曲
   */
  var nextSong: Song = EMPTY_SONG
    private set

  /**
   * MediaPlayer 负责歌曲的播放等
   */
  var mediaPlayer: IjkMediaPlayer = IjkMediaPlayer()


  /**
   * 桌面部件
   */
  private val appWidgets: HashMap<String, BaseAppwidget> = HashMap()

  /**
   * AudioManager
   */
  private val audioManager: AudioManager by lazy {
    getSystemService(Context.AUDIO_SERVICE) as AudioManager
  }

  /**
   * 播放控制的Receiver
   */
  private val controlReceiver: ControlReceiver by lazy {
    ControlReceiver()
  }

  /**
   * 事件
   */
  private val musicEventReceiver: MusicEventReceiver by lazy {
    MusicEventReceiver()
  }

  /**
   * 监测耳机拔出的Receiver
   */
  private val headSetReceiver: HeadsetPlugReceiver by lazy {
    HeadsetPlugReceiver()
  }

  /**
   * 接收桌面部件
   */
  private val widgetReceiver: WidgetReceiver by lazy {
    WidgetReceiver()
  }

  /**
   * 监听AudioFocus的改变
   */
  private val audioFocusListener by lazy {
    AudioFocusChangeListener()
  }

  /**
   * MediaSession
   */
  lateinit var mediaSession: MediaSessionCompat
    private set

  /**
   * 当前是否获得AudioFocus
   */
  private var audioFocus = false

  /**
   * 更新相关Activity的Handler
   */
  private val updateUIHandler = UpdateUIHandler(this)

  /**
   * 电源锁
   */
  private val wakeLock: PowerManager.WakeLock by lazy {
    (getSystemService(Context.POWER_SERVICE) as PowerManager)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.simpleName)
  }

  /**
   * 通知栏
   */
  private lateinit var notify: Notify

  /**
   * 当前控制命令
   */
  private var control: Int = 0

  /**
   * WindowManager 控制悬浮窗
   */
  private val windowManager: WindowManager by lazy {
    getSystemService(Context.WINDOW_SERVICE) as WindowManager
  }

  /**
   * 是否显示桌面歌词
   */
  private var showDesktopLyric = false

  /**
   * 桌面歌词控件
   */
  private var desktopLyricView: DesktopLyricView? = null

  /**
   * service是否停止运行
   */
  private var isServiceStop = true

  /**
   * handlerThread
   */
  private val playbackThread: HandlerThread by lazy {
    HandlerThread("IO")
  }
  private val playbackHandler: PlaybackHandler by lazy {
    PlaybackHandler(playbackThread.looper)
  }

  /**
   * 监听锁屏
   */
  private val screenReceiver: ScreenReceiver by lazy {
    ScreenReceiver()
  }
  private var screenOn = true

  /**
   * shortcut
   */
  private val shortcutManager: DynamicShortcutManager by lazy {
    DynamicShortcutManager(this)
  }

  /**
   * 音量控制
   */
  private val volumeController: VolumeController by lazy {
    VolumeController(this)
  }

  /**
   * 退出时播放的进度
   */
  private var lastProgress: Int = 0

  /**
   * 是否开启断点播放
   */
  private var playAtBreakPoint: Boolean = false
  private var progressTask: ProgressTask? = null

  /**
   * 操作类型
   */
  var operation = -1

  /**
   * Binder
   */
  private val musicBinder = MusicBinder()

  /**
   * 数据库
   */
  val repository = DatabaseRepository.getInstance()

  /**
   * 监听Mediastore变化
   */
  private val mediaStoreObserver: MediaStoreObserver by lazy {
    MediaStoreObserver(this)
  }
  private lateinit var service: MusicService

  private var hasPermission = false

  private var alreadyUnInit: Boolean = false
  private var speed = 1.0f


  /**
   * 获得是否正在播放
   */
  val isPlaying: Boolean
    get() = isPlay

  /**
   * 获得当前播放进度
   */
  val progress: Int
    get() {
      try {
        if (prepared) {
          return mediaPlayer.currentPosition.toInt()
        }
      } catch (e: IllegalStateException) {
        Timber.v("getProgress() %s", e.toString())
      }

      return 0
    }

  val duration: Long
    get() = if (prepared) {
      mediaPlayer.duration
    } else 0

  /**
   * 更新桌面歌词与桌面部件
   */
  private var timer: Timer = Timer()
  private var desktopLyricTask: LyricTask? = null
  private var desktopWidgetTask: WidgetTask? = null


  private var needShowDesktopLyric: Boolean = false

  /**
   * 创建桌面歌词悬浮窗
   */
  private var isDesktopLyricInitializing = false

  /**
   * 桌面歌词是否显示
   */
  val isDesktopLyricShowing: Boolean
    get() = desktopLyricView != null

  /**
   * 桌面歌词是否锁定
   */
  val isDesktopLyricLocked: Boolean
    get() = if (desktopLyricView == null)
      SPUtil.getValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_LOCK, false)
    else
      desktopLyricView?.isLocked == true


  override fun onTaskRemoved(rootIntent: Intent) {
    super.onTaskRemoved(rootIntent)
    Timber.tag(TAG_LIFECYCLE).v("onTaskRemoved")
//    unInit()
//    stopSelf()
//    System.exit(0)
  }

  override fun onDestroy() {
    Timber.tag(TAG_LIFECYCLE).v("onDestroy")
    super.onDestroy()
    isServiceStop = true
    unInit()
  }

  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(LanguageHelper.setLocal(base))
  }

  override fun onCreate() {
    super.onCreate()
    Timber.tag(TAG_LIFECYCLE).v("onCreate")
    service = this
    setUp()
  }

  override fun onBind(intent: Intent): IBinder? {
    return musicBinder
  }

  inner class MusicBinder : Binder() {
    val service: MusicService
      get() = this@MusicService
  }

  @SuppressLint("CheckResult")
  override fun onStartCommand(commandIntent: Intent?, flags: Int, startId: Int): Int {
    Timber.tag(TAG_LIFECYCLE).v("onStartCommand")
    isServiceStop = false

    Single
        .create<String> { emitter ->
          hasPermission = Util.hasPermissions(EXTERNAL_STORAGE_PERMISSIONS)
          if (!loadFinished && hasPermission) {
            load()
          }
          commandIntent?.action?.let {
            emitter.onSuccess(it)
          }
        }.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { action -> handleStartCommandIntent(commandIntent, action) }

    return Service.START_NOT_STICKY
  }

  private fun setUp() {

    HeadsetPlugReceiver.setHeadsetOn(audioManager.isWiredHeadsetOn)

    playbackThread.start()


    //电源锁
    wakeLock.setReferenceCounted(false)
    //通知栏
    notify = if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) and
        !SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.NOTIFY_STYLE_CLASSIC, false)) {
      NotifyImpl24(this)
    } else {
      NotifyImpl(this)
    }


    //桌面部件
    appWidgets[APPWIDGET_BIG] = AppWidgetBig.getInstance()
    appWidgets[APPWIDGET_MEDIUM] = AppWidgetMedium.getInstance()
    appWidgets[APPWIDGET_MEDIUM_TRANSPARENT] = AppWidgetMediumTransparent.getInstance()
    appWidgets[APPWIDGET_SMALL] = AppWidgetSmall.getInstance()
    appWidgets[APPWIDGET_SMALL_TRANSPARENT] = AppWidgetSmallTransparent.getInstance()

    //初始化Receiver
    val eventFilter = IntentFilter()
    eventFilter.addAction(MEDIA_STORE_CHANGE)
    eventFilter.addAction(PERMISSION_CHANGE)
    eventFilter.addAction(PLAYLIST_CHANGE)
    registerLocalReceiver(musicEventReceiver, eventFilter)

    registerLocalReceiver(controlReceiver, IntentFilter(ACTION_CMD))

    val noisyFilter = IntentFilter()
    noisyFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    noisyFilter.addAction(Intent.ACTION_HEADSET_PLUG)
    registerReceiver(headSetReceiver, noisyFilter)

    registerReceiver(widgetReceiver, IntentFilter(ACTION_WIDGET_UPDATE))

    val screenFilter = IntentFilter()
    screenFilter.addAction(Intent.ACTION_SCREEN_ON)
    screenFilter.addAction(Intent.ACTION_SCREEN_OFF)
    registerReceiver(screenReceiver, screenFilter)

    //监听数据库变化
    contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)
    contentResolver.registerContentObserver(Uri.parse("content://media/external/images/media"), true,
        object : ContentObserver(null) {
          override fun onChange(selfChange: Boolean, uri: Uri?) {
            Timber.v("onChange, selfChange: $selfChange uri:$uri")
          }
        })

    setUpPlayer()
    setUpSession()

    //初始化音效设置
    val i = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
    i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mediaPlayer.audioSessionId)
    if (Util.isIntentAvailable(this, i)) {
      openAudioEffectSession()
    } else {
//      EQActivity.Init()
    }

  }

  /**
   * 初始化mediasession
   */
  private fun setUpSession() {
    val mediaButtonReceiverComponentName = ComponentName(applicationContext,
        MediaButtonReceiver::class.java)

    val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
    mediaButtonIntent.component = mediaButtonReceiverComponentName

    val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, mediaButtonIntent, 0)

    mediaSession = MediaSessionCompat(applicationContext, "Cassette", mediaButtonReceiverComponentName, pendingIntent)
    mediaSession.setCallback(object : MediaSessionCompat.Callback() {
      override fun onMediaButtonEvent(event: Intent): Boolean {
        return MediaButtonReceiver.handleMediaButtonIntent(this@MusicService, event)
      }

      override fun onSkipToNext() {
        Timber.v("onSkipToNext")
        playNext()
      }

      override fun onSkipToPrevious() {
        Timber.v("onSkipToPrevious")
        playPrevious()
      }

      override fun onPlay() {
        Timber.v("onPlay")
        play(true)
      }

      override fun onPause() {
        Timber.v("onPause")
        pause(false)
      }

      override fun onStop() {
        stopSelf()
      }

      override fun onSeekTo(pos: Long) {
        setProgress(pos)
      }
    })

    mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
    mediaSession.setMediaButtonReceiver(pendingIntent)
    mediaSession.isActive = true
  }

  /**
   * 初始化Mediaplayer
   */
  private fun setUpPlayer() {
    mediaPlayer = IjkMediaPlayer()

    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
    mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)

    mediaPlayer.setOnCompletionListener { mp ->
      if (playModel == PLAY_REPEAT) {
        prepare(currentSong.url)
      } else {
        playNextOrPrev(true)
      }
      operation = Command.NEXT
      acquireWakeLock()
    }
    mediaPlayer.setOnPreparedListener { mp ->
      Timber.v("Ready to finish:%s", firstPrepared)
      if (firstPrepared) {
        firstPrepared = false
        if (lastProgress > 0) {
          mediaPlayer.seekTo(lastProgress.toLong())
        }
        return@setOnPreparedListener
      }
      Timber.v("Start playing")
      //记录播放历史
      updatePlayHistory()
      play(false)
    }

    mediaPlayer.setOnErrorListener { mp, what, extra ->
      try {
        prepared = false
        mediaPlayer.release()
        setUpPlayer()
        ToastUtil.show(service, R.string.mediaplayer_error, what, extra)
        return@setOnErrorListener true
      } catch (ignored: Exception) {

      }

      false
    }
  }


  /**
   * 更新播放历史
   */
  private fun updatePlayHistory() {
    repository.updateHistory(currentSong)
        .compose(applySingleScheduler())
        .subscribe(LogObserver())
  }

  /**
   * 初始化mediaplayer
   */
  private fun setUpDataSource(item: Song?, pos: Int) {
    if (item == null) {
      return
    }
    //初始化当前播放歌曲
    Timber.v("Current song:%s", item.title)
    currentSong = item
    currentId = currentSong.id
    currentIndex = pos
    prepare(currentSong.url, false)
    //桌面歌词
    //        updateDesktopLyric();
    //初始化下一首歌曲
    //        updateNextSong();
    if (playModel == PLAY_SHUFFLE) {
      makeShuffleList(currentId)
    }
    //查找上次退出时保存的下一首歌曲是否还存在
    //如果不存在，重新设置下一首歌曲
    nextId = SPUtil
        .getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.NEXT_SONG_ID, -1)
    if (nextId == -1) {
      nextIndex = currentIndex
      updateNextSong()
    } else {
      nextIndex = if (playModel != PLAY_SHUFFLE)
        playQueue.indexOf(nextId)
      else
        randomQueue.indexOf(nextId)
      nextSong = MediaStoreUtil.getSongById(nextId)
      if (nextSong == EMPTY_SONG) {
        return
      }
      updateNextSong()
    }
  }

  private fun unInit() {
    if (alreadyUnInit) {
      return
    }
    closeAudioEffectSession()
    if (isPlaying) {
      pause(false)
    }
    mediaPlayer.release()
    loadFinished = false
    prepared = false
    shortcutManager.updateContinueShortcut(this)

    timer.cancel()
    notify.cancelPlayingNotify()

    removeDesktopLyric()

    updateUIHandler.removeCallbacksAndMessages(null)
    showDesktopLyric = false

    if (Build.VERSION.SDK_INT >= 18) {
      playbackThread.quitSafely()
    } else {
      playbackThread.quit()
    }

    audioManager.abandonAudioFocus(audioFocusListener)
    mediaSession.isActive = false
    mediaSession.release()

    unregisterLocalReceiver(controlReceiver)
    unregisterLocalReceiver(musicEventReceiver)
    Util.unregisterReceiver(this, headSetReceiver)
    Util.unregisterReceiver(this, screenReceiver)
    Util.unregisterReceiver(this, widgetReceiver)

    releaseWakeLock()
    contentResolver.unregisterContentObserver(mediaStoreObserver)

    ShakeDetector.getInstance().stopListen()

    alreadyUnInit = true
  }

  private fun closeAudioEffectSession() {
    val audioEffectsIntent = Intent(
        AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mediaPlayer.audioSessionId)
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
    sendBroadcast(audioEffectsIntent)
  }

  private fun openAudioEffectSession() {
    val audioEffectsIntent = Intent(
        AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mediaPlayer.audioSessionId)
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
    sendBroadcast(audioEffectsIntent)
  }


  fun setAllSong(allSong: List<Int>?) {
    allSong?.let {
      this.allSong.clear()
      this.allSong.addAll(it)
    }
  }

  /**
   * 设置播放队列
   */
  @WorkerThread
  @Synchronized
  fun setPlayQueue(newQueueList: List<Int>?) {
    if (newQueueList == null || newQueueList.isEmpty()) {
      return
    }
    if (newQueueList == playQueue) {
      return
    }
    playQueue.clear()
    playQueue.addAll(newQueueList)

    repository.runInTransaction(Runnable {
      repository.clearPlayQueue()
          .concatWith(repository.insertToPlayQueue(playQueue))
          .subscribe()
    })
  }

  /**
   * 设置播放队列
   */
  @WorkerThread
  @Synchronized
  fun setPlayQueue(newQueueList: List<Int>?, intent: Intent) {
    //当前模式是随机播放 或者即将设置为随机播放 都要更新mRandomList
    val shuffle = intent.getBooleanExtra("shuffle", false) || SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_MODEL,
        PLAY_LOOP) == PLAY_SHUFFLE
    if (newQueueList == null || newQueueList.isEmpty()) {
      return
    }
    //设置的播放队列相等
    val equals = newQueueList == playQueue
    if (!equals) {
      playQueue.clear()
      playQueue.addAll(newQueueList)
    }

    if (shuffle) {
      playModel = PLAY_SHUFFLE
      updateNextSong()
    }
    controlReceiver.onReceive(this, intent)

    if (equals) {
      return
    }

    repository.runInTransaction(Runnable {
      repository.clearPlayQueue()
          .concatWith(repository.insertToPlayQueue(playQueue))
          .subscribe()
    })

  }

  private fun setPlay(isPlay: Boolean) {
    this.isPlay = isPlay
    updateUIHandler.sendEmptyMessage(UPDATE_PLAY_STATE)
    //        sendLocalBroadcast(new Intent(PLAY_STATE_CHANGE));
  }

  /**
   * 播放下一首
   */
  override fun playNext() {
    playNextOrPrev(true)
  }

  /**
   * 播放上一首
   */
  override fun playPrevious() {
    playNextOrPrev(false)
  }

  /**
   * 开始播放
   */
  override fun play(fadeIn: Boolean) {
    audioFocus = audioManager.requestAudioFocus(
        audioFocusListener,
        AudioManager.STREAM_MUSIC,
        AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    if (!audioFocus) {
      return
    }
    setPlay(true)
    //eq可用
    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
    intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mediaPlayer.audioSessionId)
    if (Util.isIntentAvailable(this, intent)) {
      openAudioEffectSession()
    }
    //倍速播放
    setSpeed(speed)
    //更新所有界面
    updateUIHandler.sendEmptyMessage(UPDATE_META_DATA)
    mediaPlayer.start()
    if (fadeIn) {
      volumeController.fadeIn()
    } else {
      volumeController.directTo(1f)
    }

    playbackHandler.post {
      //保存当前播放和下一首播放的歌曲的id
      SPUtil
          .putValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_SONG_ID, currentId)
      SPUtil.putValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.NEXT_SONG_ID, nextId)
    }
  }


  /**
   * 根据当前播放状态暂停或者继续播放
   */
  override fun toggle() {
    if (mediaPlayer.isPlaying) {
      pause(false)
    } else {
      play(true)
    }
  }

  /**
   * 暂停
   */
  override fun pause(updateMediasessionOnly: Boolean) {
    if (updateMediasessionOnly) {
      updateMediaSession(operation)
    } else {
      if (!isPlaying) { //如果当前已经暂停了 就不重复操作了 避免已经关闭了通知栏又再次显示
        return
      }
      setPlay(false)
      updateUIHandler.sendEmptyMessage(UPDATE_META_DATA)
      volumeController.fadeOut()
    }
  }

  /**
   * 播放选中的歌曲 比如在全部歌曲或者专辑详情里面选中某一首歌曲
   *
   * @param position 播放位置
   */
  override fun playSelectSong(position: Int) {
    //        final List<Integer> playQueue = new ArrayList<>(playQueue);
    //        final List<Integer> randomQueue = new ArrayList<>(randomQueue);

    currentIndex = position
    if (currentIndex == -1 || currentIndex >= playQueue.size) {
      ToastUtil.show(service, R.string.illegal_arg)
      return
    }
    currentId = playQueue[currentIndex]
    currentSong = MediaStoreUtil.getSongById(currentId)

    nextIndex = currentIndex
    nextId = currentId

    try {
      //如果是随机播放 需要调整下RandomQueue
      //保证正常播放队列和随机播放队列中当前歌曲的索引一致
      val index = randomQueue.indexOf(currentId)
      if (playModel == PLAY_SHUFFLE &&
          index != currentIndex &&
          index > 0) {
        Collections.swap(randomQueue, currentIndex, index)
      }
    } catch (e: Exception) {
    }

    if (currentSong.url.isNullOrEmpty()) {
      ToastUtil.show(service, R.string.song_lose_effect)
      return
    }
    //        isPlay = true;
    prepare(currentSong.url)
    updateNextSong()
  }

  override fun onMediaStoreChanged() {

  }

  override fun onPermissionChanged(has: Boolean) {
    if (has != hasPermission && has) {
      hasPermission = true
      loadSync()
    }
  }

  override fun onPlayListChanged(name: String) {
    if (name == PlayQueue.TABLE_NAME) {
      repository
          .getPlayQueue()
          .compose(applySingleScheduler())
          .subscribe { ids ->
            playQueue.clear()
            playQueue.addAll(ids)

            // 如果下一首歌曲不在队列里面 重新设置下一首歌曲
            if (!playQueue.contains(nextId)) {
              Timber.v("Reset the next song after the play queue is changed")
              updateNextSong()
              //todo 更新播放界面下一首
            }

            // 随机播放模式下重新设置下RandomQueue
            if (playModel == PLAY_SHUFFLE) {
              Timber.v("Reset the random queue after the play queue is changed")
              makeShuffleList(currentId)
            }
          }
    }
  }

  override fun onMetaChanged() {

  }

  override fun onPlayStateChange() {

  }

  override fun onServiceConnected(service: MusicService) {

  }

  override fun onServiceDisConnected() {

  }

  inner class WidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
      //            final int skin = SPUtil.getValue(context,SPUtil.SETTING_KEY.NAME,SPUtil.SETTING_KEY.APP_WIDGET_SKIN,SKIN_WHITE_1F);
      //            SPUtil.putValue(context,SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.APP_WIDGET_SKIN,skin == SKIN_WHITE_1F ? SKIN_TRANSPARENT : SKIN_WHITE_1F);

      val name = intent.getStringExtra(BaseAppwidget.EXTRA_WIDGET_NAME)
      val appIds = intent.getIntArrayExtra(BaseAppwidget.EXTRA_WIDGET_IDS)
      when (name) {
        APPWIDGET_BIG -> if (appWidgets[APPWIDGET_BIG] != null) {
          appWidgets[APPWIDGET_BIG]?.updateWidget(service, appIds, true)
        }
        APPWIDGET_MEDIUM -> if (appWidgets[APPWIDGET_MEDIUM] != null) {
          appWidgets[APPWIDGET_MEDIUM]?.updateWidget(service, appIds, true)
        }
        APPWIDGET_SMALL -> if (appWidgets[APPWIDGET_SMALL] != null) {
          appWidgets[APPWIDGET_SMALL]?.updateWidget(service, appIds, true)
        }
        APPWIDGET_MEDIUM_TRANSPARENT -> if (appWidgets[APPWIDGET_MEDIUM_TRANSPARENT] != null) {
          appWidgets[APPWIDGET_MEDIUM_TRANSPARENT]?.updateWidget(service, appIds, true)
        }
        APPWIDGET_SMALL_TRANSPARENT -> if (appWidgets[APPWIDGET_SMALL_TRANSPARENT] != null) {
          appWidgets[APPWIDGET_SMALL_TRANSPARENT]?.updateWidget(service, appIds, true)
        }
      }
    }
  }

  inner class MusicEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
      handleMusicEvent(intent)
    }
  }

  private fun handleStartCommandIntent(commandIntent: Intent?, action: String) {
    firstPrepared = false
    when (action) {
      ACTION_APPWIDGET_OPERATE -> {
        val appwidgetIntent = Intent(ACTION_CMD)
        val control = commandIntent?.getIntExtra(EXTRA_CONTROL, -1)
        if (control == UPDATE_APPWIDGET) {
//          int skin = SPUtil.getValue(this,SPUtil.SETTING_KEY.SETTING_,SPUtil.SETTING_KEY.APP_WIDGET_SKIN,SKIN_WHITE_1F);
//          SPUtil.putValue(this,SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.APP_WIDGET_SKIN,skin == SKIN_WHITE_1F ? SKIN_TRANSPARENT : SKIN_WHITE_1F);
          updateAppwidget()
        } else {
          appwidgetIntent.putExtra(EXTRA_CONTROL, control)
          controlReceiver.onReceive(this, appwidgetIntent)
        }
      }
      ACTION_SHORTCUT_CONTINUE_PLAY -> {
        val continueIntent = Intent(ACTION_CMD)
        continueIntent.putExtra(EXTRA_CONTROL, Command.TOGGLE)
        controlReceiver.onReceive(this, continueIntent)
      }
      ACTION_SHORTCUT_SHUFFLE -> {
        if (playModel != PLAY_SHUFFLE) {
          playModel = PLAY_SHUFFLE
        }
        val shuffleIntent = Intent(ACTION_CMD)
        shuffleIntent.putExtra(EXTRA_CONTROL, Command.NEXT)
        controlReceiver.onReceive(this, shuffleIntent)
      }
      ACTION_SHORTCUT_MYLOVE -> {
        val myLoveIds = repository.getMyLoveList().blockingGet()
        if (myLoveIds == null || myLoveIds.isEmpty()) {
          ToastUtil.show(service, R.string.list_is_empty)
          return
        }
        val myloveIntent = Intent(ACTION_CMD)
        myloveIntent.putExtra(EXTRA_CONTROL, Command.PLAYSELECTEDSONG)
        myloveIntent.putExtra(EXTRA_POSITION, 0)
        setPlayQueue(myLoveIds, myloveIntent)
      }
      ACTION_SHORTCUT_LASTADDED -> {
        val songs = MediaStoreUtil.getLastAddedSong()
        val lastAddIds = ArrayList<Int>()
        if (songs == null || songs.size == 0) {
          ToastUtil.show(service, R.string.list_is_empty)
          return
        }
        for (song in songs) {
          lastAddIds.add(song.id)
        }
        val lastedIntent = Intent(ACTION_CMD)
        lastedIntent.putExtra(EXTRA_CONTROL, Command.PLAYSELECTEDSONG)
        lastedIntent.putExtra(EXTRA_POSITION, 0)
        setPlayQueue(lastAddIds, lastedIntent)
      }
      else -> if (action.equals(ACTION_CMD, ignoreCase = true)) {
        controlReceiver.onReceive(this, commandIntent)
      }
    }
  }

  private fun handleMusicEvent(intent: Intent?) {
    if (intent == null) {
      return
    }
    val action = intent.action
    when {
      MEDIA_STORE_CHANGE == action -> onMediaStoreChanged()
      PERMISSION_CHANGE == action -> onPermissionChanged(intent.getBooleanExtra(EXTRA_PERMISSION, false))
      PLAYLIST_CHANGE == action -> onPlayListChanged(intent.getStringExtra(EXTRA_PLAYLIST))
    }
  }

  private fun handleMetaChange() {
    if (currentSong == EMPTY_SONG) {
      return
    }
    updateAppwidget()
    if (needShowDesktopLyric) {
      showDesktopLyric = true
      needShowDesktopLyric = false
    }
    updateDesktopLyric(false)
    updateNotification()
    updateMediaSession(operation)
    // 是否需要保存进度
    if (playAtBreakPoint) {
      startSaveProgress()
    }

    sendLocalBroadcast(Intent(MusicService.META_CHANGE))
  }

  private fun updateNotification() {
    notify.updateForPlaying()
  }

  private fun handlePlayStateChange() {
    if (currentSong == EMPTY_SONG) {
      return
    }
    //更新桌面歌词播放按钮
    desktopLyricView?.setPlayIcon(isPlaying)
    shortcutManager.updateContinueShortcut(this)
    sendLocalBroadcast(Intent(MusicService.PLAY_STATE_CHANGE))
  }

  /**
   * 接受控制命令 包括暂停、播放、上下首、改版播放模式等
   */
  inner class ControlReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
      Timber.v("ControlReceiver: %s", intent)
      if (intent == null || intent.extras == null) {
        return
      }
      val control = intent.getIntExtra(EXTRA_CONTROL, -1)
      this@MusicService.control = control

      if (control == Command.PLAYSELECTEDSONG || control == Command.PREV || control == Command.NEXT
          || control == Command.TOGGLE || control == Command.PAUSE || control == Command.START) {
        //保存控制命令,用于播放界面判断动画
        operation = control
        if (playQueue.size == 0) {
          //列表为空，尝试读取
          repository.getPlayQueue()
              .compose(applySingleScheduler())
              .subscribe(Consumer {
                playQueue.clear()
                playQueue.addAll(it)
              })
        }
      }

      when (control) {
        //关闭通知栏
        Command.CLOSE_NOTIFY -> {
          Notify.isNotifyShowing = false
          //          if (notify instanceof NotifyImpl24) { //仅仅只是设置标志位
          //            return;
          //          }
          pause(false)
          needShowDesktopLyric = true
          showDesktopLyric = false
          updateUIHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC)
          stopUpdateLyric()
          updateUIHandler.postDelayed({ notify.cancelPlayingNotify() }, 300)
        }
        //播放选中的歌曲
        Command.PLAYSELECTEDSONG -> {
          playSelectSong(intent.getIntExtra(EXTRA_POSITION, -1))
        }
        //播放上一首
        Command.PREV -> {
          playPrevious()
        }
        //播放下一首
        Command.NEXT -> {
          playNext()
        }
        //暂停或者继续播放
        Command.TOGGLE -> {
          toggle()
        }
        //暂停
        Command.PAUSE -> {
          pause(false)
        }
        //继续播放
        Command.START -> {
          play(false)
        }
        //改变播放模式
        Command.CHANGE_MODEL -> {
          playModel = if (playModel == PLAY_REPEAT) PLAY_LOOP else playModel + 1
        }
        //取消或者添加收藏
        Command.LOVE -> {
          repository.toggleMyLove(currentId)
              .compose(applySingleScheduler())
              .subscribe({
                if (it) {
                  isLove = !isLove
                  updateAppwidget()
                }
              }, {
                Timber.v(it)
              })
        }
        //桌面歌词
        Command.TOGGLE_DESKTOP_LYRIC -> {
          val open: Boolean = if (intent.hasExtra(EXTRA_DESKTOP_LYRIC)) {
            intent.getBooleanExtra(EXTRA_DESKTOP_LYRIC, false)
          } else {
            !SPUtil.getValue(service,
                SPUtil.SETTING_KEY.NAME,
                SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW, false)
          }
          if (open && !FloatWindowManager.getInstance().checkPermission(service)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
              val permissionIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
              permissionIntent.data = Uri.parse("package:$packageName")
              permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              if (Util.isIntentAvailable(service, permissionIntent)) {
                startActivity(permissionIntent)
              }
            }
            ToastUtil.show(service, R.string.plz_give_float_permission)
            return
          }
          SPUtil.putValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW,
              open)
          if (showDesktopLyric != open) {
            showDesktopLyric = open
            ToastUtil.show(service, if (showDesktopLyric) R.string.opened_desktop_lrc else R.string.closed_desktop_lrc)
            if (showDesktopLyric) {
              updateDesktopLyric(false)
            } else {
              closeDesktopLyric()
            }
          }
        }
        Command.TOGGLE_MEDIASESSION ->
          when (SPUtil.getValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCKSCREEN, CASSETTE_LOCKSCREEN)) {
            CASSETTE_LOCKSCREEN, CLOSE_LOCKSCREEN -> cleanMetaData()
            SYSTEM_LOCKSCREEN -> updateMediaSession(Command.NEXT)
          }
        //临时播放一首歌曲
        Command.PLAY_TEMP -> {
          intent.getParcelableExtra<Song>(EXTRA_SONG)?.let {
            currentSong = it
            operation = Command.PLAY_TEMP
            prepare(currentSong.url)
          }
        }
        //切换通知栏样式
        Command.TOGGLE_NOTIFY -> {
          val classic = intent.getBooleanExtra(SPUtil.SETTING_KEY.NOTIFY_STYLE_CLASSIC, false)
          notify = if (classic) {
            NotifyImpl(this@MusicService)
          } else {
            NotifyImpl24(this@MusicService)
          }
          if (Notify.isNotifyShowing) {
            updateNotification()
          }
        }
        //解锁桌面歌词
        Command.UNLOCK_DESKTOP_LYRIC -> {
          if (desktopLyricView != null) {
            desktopLyricView?.saveLock(false, true)
          } else {
            SPUtil.putValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_LOCK, false)
          }
          //更新通知栏
          updateNotification()
        }
        //锁定通知栏
        Command.LOCK_LYRIC ->
          //更新通知栏
          updateNotification()
        //某一首歌曲添加至下一首播放
        Command.ADD_TO_NEXT_SONG -> {
          val nextSong = intent.getParcelableExtra<Song>(EXTRA_SONG) ?: return
          //添加到播放队列
          if (nextId == nextSong.id) {
            ToastUtil.show(service, R.string.already_add_to_next_song)
            return
          }
          //根据当前播放模式，添加到队列
          if (playModel == PLAY_SHUFFLE) {
            if (randomQueue.contains(nextSong.id)) {
              randomQueue.remove(Integer.valueOf(nextSong.id))
              randomQueue.add(if (currentIndex + 1 < randomQueue.size) currentIndex + 1 else 0,
                  nextSong.id)
            } else {
              randomQueue.add(randomQueue.indexOf(currentId) + 1, nextSong.id)
            }
          } else {
            if (playQueue.contains(nextSong.id)) {
              playQueue.remove(Integer.valueOf(nextSong.id))
              playQueue.add(if (currentIndex + 1 < playQueue.size) currentIndex + 1 else 0,
                  nextSong.id)
            } else {
              playQueue.add(playQueue.indexOf(currentId) + 1, nextSong.id)
            }
          }

          //更新下一首
          nextIndex = currentIndex
          updateNextSong()
          //保存到数据库
          if (playModel != PLAY_SHUFFLE) {
            Single
                .fromCallable {
                  repository.runInTransaction(Runnable {
                    repository.clearPlayQueue()
                    repository.insertToPlayQueue(playQueue)
                  })
                  true
                }
                .compose(applySingleScheduler())
                .subscribe({ success -> ToastUtil.show(service, R.string.already_add_to_next_song) },
                    { throwable -> ToastUtil.show(service, R.string.play_failed) })
          }
        }
        //改变歌词源
        Command.CHANGE_LYRIC -> if (showDesktopLyric) {
          updateDesktopLyric(true)
        }
        //断点播放
        Command.PLAY_AT_BREAKPOINT -> {
          playAtBreakPoint = intent.getBooleanExtra(SPUtil.SETTING_KEY.PLAY_AT_BREAKPOINT, false)
          if (!playAtBreakPoint) {
            SPUtil.putValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_PLAY_PROGRESS, 0)
            stopSaveProgress()
          } else {
            startSaveProgress()
          }
        }
        //切换定时器
        Command.TOGGLE_TIMER -> {
          val hasDefault = SPUtil.getValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DEFAULT, false)
          if (!hasDefault) {
            ToastUtil.show(service, getString(R.string.plz_set_default_time))
          }
          val time = SPUtil.getValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DURATION, -1)
          SleepTimer.toggleTimer((time * 1000).toLong())
        }
        //耳机拔出
        Command.HEADSET_CHANGE -> if (isPlaying) {
          pause(false)
        }
        else -> {
        }
      }
    }
  }

  /**
   * 是否只需要更新播放状态,比如暂停
   */
  private fun updatePlayStateOnly(cmd: Int): Boolean {
    return cmd == Command.PAUSE || cmd == Command.START || cmd == Command.TOGGLE
  }

  private fun updateAllView(cmd: Int): Boolean {
    return (cmd == Command.PLAYSELECTEDSONG || cmd == Command.PREV || cmd == Command.NEXT
        || cmd == Command.PLAY_TEMP)
  }

  /**
   * 清除锁屏显示的内容
   */
  private fun cleanMetaData() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      mediaSession.setMetadata(MediaMetadataCompat.Builder().build())
      mediaSession.setPlaybackState(
          PlaybackStateCompat.Builder().setState(PlaybackState.STATE_NONE, 0, 1f).build())
    }
  }


  /**
   * 更新锁屏
   */
  private fun updateMediaSession(control: Int) {
    if (currentSong == EMPTY_SONG) {
      mediaSession.setMetadata(null)
      return
    }
    val isSmartisan = Build.MANUFACTURER.equals("smartisan", ignoreCase = true)
    if (!isSmartisan && SPUtil.getValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCKSCREEN, CASSETTE_LOCKSCREEN) == CLOSE_LOCKSCREEN) {
      mediaSession.setMetadata(null)
      return
    }

    val builder = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentSong.album)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.artist)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, currentSong.artist)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, currentSong.displayname)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentSong.duration)
        .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (currentIndex + 1).toLong())
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.title)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      builder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, playQueue.size.toLong())
    }

    if (updatePlayStateOnly(control)) {
      mediaSession.setMetadata(builder.build())
    } else {
      object : RemoteUriRequest(getSearchRequestWithAlbumType(currentSong),
          RequestConfig.Builder(400, 400).build()) {
        override fun onError(throwable: Throwable) {
          setMediaSessionData(null)
        }

        override fun onSuccess(result: Bitmap?) {
          setMediaSessionData(result)
        }

        private fun setMediaSessionData(result: Bitmap?) {
          val bitmap = copy(result)
          builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
          mediaSession.setMetadata(builder.build())
        }
      }.load()
    }

    mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
        .setState(if (isPlay) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED, progress.toLong(), speed)
        .setActions(MEDIA_SESSION_ACTIONS).build())
  }

  /**
   * 准备播放
   *
   * @param path 播放歌曲的路径
   */
  private fun prepare(path: String, requestFocus: Boolean = true) {
    Timber.v("Ready to play: %s", path)
    if (TextUtils.isEmpty(path)) {
      ToastUtil.show(service, getString(R.string.path_empty))
      return
    }
    if (requestFocus) {
      audioFocus = audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC,
          AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
      if (!audioFocus) {
        ToastUtil.show(service, getString(R.string.cant_request_audio_focus))
        return
      }
    }
    if (isPlaying) {
      pause(true)
    }
    prepared = false
    //    openAudioEffectSession();

    playbackHandler.post {
      try {
        isLove = repository.isMyLove(currentId)
            .onErrorReturn {
              false
            }
            .blockingGet()
        mediaPlayer.reset()
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0)
        mediaPlayer.dataSource = path
        mediaPlayer.prepareAsync()
        prepared = true
      } catch (e: Exception) {
        ToastUtil.show(service, getString(R.string.play_failed) + e.toString())
        prepared = false
      }
    }
  }

  /**
   * 根据当前播放模式，切换到上一首或者下一首
   *
   * @param isNext 是否是播放下一首
   */
  fun playNextOrPrev(isNext: Boolean) {
    if (playQueue.size == 0) {
      ToastUtil.show(service, getString(R.string.list_is_empty))
      return
    }
    Timber.v("Play the next song")
    if (isNext) {
      //如果是点击下一首 播放预先设置好的下一首歌曲
      currentId = nextId
      currentIndex = nextIndex
      currentSong = Song(nextSong)
    } else {
      val queue = ArrayList(if (playModel == PLAY_SHUFFLE)
        randomQueue
      else
        playQueue)
      //如果点击上一首
      if (--currentIndex < 0) {
        currentIndex = queue.size - 1
      }
      if (currentIndex == -1 || currentIndex > queue.size - 1) {
        return
      }
      currentId = queue[currentIndex]

      currentSong = MediaStoreUtil.getSongById(currentId)
      nextIndex = currentIndex
      nextId = currentId
    }
    if (currentSong == EMPTY_SONG) {
      ToastUtil.show(service, R.string.song_lose_effect)
      return
    }
    updateNextSong()
    setPlay(true)
    prepare(currentSong.url)

  }

  /**
   * 更新下一首歌曲
   */
  fun updateNextSong() {
    if (playQueue.size == 0) {
      ToastUtil.show(service, R.string.list_is_empty)
      return
    }

    if (playModel == PLAY_SHUFFLE) {
      if (randomQueue.size == 0) {
        makeShuffleList(currentId)
      }
      if (++nextIndex >= randomQueue.size) {
        nextIndex = 0
      }
      nextId = randomQueue[nextIndex]
    } else {
      if (++nextIndex >= playQueue.size) {
        nextIndex = 0
      }
      nextId = playQueue[nextIndex]
    }
    nextSong = MediaStoreUtil.getSongById(nextId)
  }

  /**
   * 生成随机播放列表
   */
  private fun makeShuffleList(current: Int) {
    randomQueue.clear()
    randomQueue.addAll(playQueue)
    if (randomQueue.isEmpty()) {
      return
    }
    //        if (current >= 0) {
    //            boolean removed = randomQueue.remove(Integer.valueOf(current));
    //            Collections.shuffle(randomQueue);
    //            if(removed)
    //                randomQueue.add(0,current);
    //        } else {
    //            Collections.shuffle(randomQueue);
    //        }
    randomQueue.shuffle()
  }

  /**
   * 设置MediaPlayer播放进度
   */
  fun setProgress(current: Long) {
    if (prepared) {
      mediaPlayer.seekTo(current)
    }
  }

  fun setSpeed(speed: Float) {
    if (prepared) {
      this.speed = speed
      mediaPlayer.setSpeed(this.speed)
    }
  }

  /**
   * 读取歌曲id列表与播放队列
   */
  private fun loadSync() {
    playbackHandler.post { this.load() }
  }

  @WorkerThread
  @Synchronized
  private fun load() {
    //迁移数据
    Migration.migrationDatabase(this)

    val isFirst = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SETTING_KEY.FIRST_LOAD, true)
    SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SETTING_KEY.FIRST_LOAD, false)
    //读取歌曲id
    allSong.clear()
    allSong.addAll(MediaStoreUtil.getAllSongsId())
    //第一次启动软件
    if (isFirst) {
      //新建我的收藏
      repository.insertPlayList(getString(R.string.my_favorite)).subscribe(object : LogObserver() {
        override fun onSuccess(value: Any) {
          super.onSuccess(value)
        }

        override fun onError(e: Throwable) {
          super.onError(e)
        }
      })

      //通知栏样式
      SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.NOTIFY_STYLE_CLASSIC,
          Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
    } else {
      //播放模式
      playModel = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_MODEL,
          PLAY_LOOP)
      //读取播放列表
      playQueue.clear()
      playQueue.addAll(repository.getPlayQueue().blockingGet())

      showDesktopLyric = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW, false)
    }

    if (playQueue.isEmpty()) {
      //默认全部歌曲为播放列表
      setPlayQueue(allSong)
    }

    //摇一摇
    if (SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SHAKE, false)) {
      ShakeDetector.getInstance().beginListen()
    }
    //播放倍速
    speed = java.lang.Float.parseFloat(
        SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SPEED, "1.0"))
    restoreLastSong()
    loadFinished = true
    updateUIHandler.postDelayed({ sendLocalBroadcast(Intent(META_CHANGE)) }, 400)
    //        sendLocalBroadcast(new Intent(ACTION_LOAD_FINISH).putExtra("Song", currentSong));
    //        openAudioEffectSession();
  }


  /**
   * 初始化上一次退出时时正在播放的歌曲
   */
  private fun restoreLastSong() {
    if (playQueue.size == 0) {
      return
    }
    //读取上次退出时正在播放的歌曲的id
    val lastId = SPUtil
        .getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_SONG_ID, -1)
    //上次退出时正在播放的歌曲是否还存在
    var isLastSongExist = false
    //上次退出时正在播放的歌曲的pos
    var pos = 0
    //查找上次退出时的歌曲是否还存在
    if (lastId != -1) {
      for (i in playQueue.indices) {
        if (lastId == playQueue[i]) {
          isLastSongExist = true
          pos = i
          break
        }
      }
    }

    var item: Song
    playAtBreakPoint = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_AT_BREAKPOINT, false)
    lastProgress = if (playAtBreakPoint)
      SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_PLAY_PROGRESS, 0)
    else
      0
    //上次退出时保存的正在播放的歌曲未失效
    item = MediaStoreUtil.getSongById(lastId)
    if (isLastSongExist && item != null) {
      setUpDataSource(item, pos)
    } else {
      lastProgress = 0
      SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_PLAY_PROGRESS, 0)
      //重新找到一个歌曲id
      var id = playQueue[0]
      for (i in playQueue.indices) {
        id = playQueue[i]
        if (id != lastId) {
          break
        }
      }
      item = MediaStoreUtil.getSongById(id)
      SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_SONG_ID, id)
      setUpDataSource(item, 0)
    }
  }

  fun deleteSongFromService(deleteSongs: List<Song>?) {
    if (deleteSongs != null && deleteSongs.isNotEmpty()) {
      val ids = ArrayList<Int>()
      for (song in deleteSongs) {
        ids.add(song.id)
      }
      allSong.removeAll(ids)
      playQueue.removeAll(ids)
    }
  }

  /**
   * 释放电源锁
   */
  private fun releaseWakeLock() {
    if (wakeLock.isHeld) {
      wakeLock.release()
    }
  }

  /**
   * 获得电源锁
   */
  private fun acquireWakeLock() {
    wakeLock.acquire(if (currentSong != EMPTY_SONG) currentSong.duration else 30000L)
  }

  private fun updateDesktopLyric(force: Boolean) {
    if (!showDesktopLyric) {
      return
    }
    if (checkNoPermission()) { //没有权限
      return
    }
    if (!isPlaying) {
      stopUpdateLyric()
    } else {
      //屏幕点亮才更新
      if (screenOn) {
        desktopLyricTask?.force = force
        startUpdateLyric()
      }
    }

  }

  /**
   * 判断是否有悬浮窗权限 没有权限关闭桌面歌词
   */
  private fun checkNoPermission(): Boolean {
    try {
      if (!FloatWindowManager.getInstance().checkPermission(service)) {
        closeDesktopLyric()
        return true
      }
      return false
    } catch (e: Exception) {
      Timber.v(e)
    }

    return true
  }

  /**
   * 更新桌面部件
   */
  private fun updateAppwidget() {
    //暂停停止更新进度条和时间
    if (!isPlaying) {
      //暂停后不再更新
      //所以需要在停止前更新一次 保证桌面部件控件的播放|暂停按钮状态是对的
      desktopWidgetTask?.run()
      stopUpdateAppWidget()
    } else {
      if (screenOn) {
        appWidgets.forEach {
          it.value.updateWidget(this, null, true)
        }
        //开始播放后更新进度条和时间
        startUpdateAppWidget()
      }
    }
  }

  private fun stopUpdateAppWidget() {
    desktopWidgetTask?.cancel()
    desktopWidgetTask = null
  }

  private fun startUpdateAppWidget() {
    if (desktopWidgetTask != null) {
      return
    }
    desktopWidgetTask = WidgetTask()
    timer.schedule(desktopWidgetTask, 1000, 1000)
  }


  private fun startUpdateLyric() {
    if (desktopLyricTask != null) {
      return
    }
    desktopLyricTask = LyricTask()
    timer.schedule(desktopLyricTask, 1000, LYRIC_FIND_INTERVAL)
  }

  private fun stopUpdateLyric() {
    desktopLyricTask?.cancel()
    desktopLyricTask = null
  }

  private inner class WidgetTask : TimerTask() {
    private val tag: String = WidgetTask::class.java.simpleName

    override fun run() {
      val isAppOnForeground = isAppOnForeground()
      if (!isAppOnForeground) { //app在前台也不用更新
        appWidgets.forEach {
          updateUIHandler.post {
            it.value.partiallyUpdateWidget(service)
          }
        }
      } else {
        Timber.v("App does not need to be updated at the foreground")
      }
    }

    override fun cancel(): Boolean {
      Timber.tag(tag).v("Stop updating desktop lyrics")
      return super.cancel()
    }
  }

  fun setLyricOffset(offset: Int) {
    desktopLyricTask?.lyricHolder?.offset = offset
  }

  private inner class LyricTask : TimerTask() {
    private var songInLyricTask = Song.EMPTY_SONG
    private val tag = LyricTask::class.java.simpleName
    val lyricHolder = LyricHolder(this@MusicService)
    var force = false

    override fun run() {
      if (songInLyricTask != currentSong) {
        songInLyricTask = currentSong
        lyricHolder.updateLyricRows(songInLyricTask)
        Timber.tag(tag).v("Regain lyrics")
        return
      }
      if (force) {
        force = false
        lyricHolder.updateLyricRows(songInLyricTask)
        Timber.tag(tag).v("Forced to regain lyrics")
        return
      }
//      Timber.tag(tag).v("更新桌面歌词")
      //判断权限
      if (checkNoPermission()) {
        return
      }
      if (isServiceStop) {
        updateUIHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC)
        return
      }
      //当前应用在前台
      if (isAppOnForeground()) {
        if (isDesktopLyricShowing) {
          updateUIHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC)
        }

      } else {
        if (!isDesktopLyricShowing) {
          updateUIHandler.removeMessages(CREATE_DESKTOP_LRC)
          Timber.tag(tag).v("Request to create desktop lyrics")
          updateUIHandler.sendEmptyMessageDelayed(CREATE_DESKTOP_LRC, 50)
        } else {
          updateUIHandler.obtainMessage(UPDATE_DESKTOP_LRC_CONTENT, lyricHolder.findCurrentLyric()).sendToTarget()
        }
      }
    }

    override fun cancel(): Boolean {
      lyricHolder.dispose()
//      updateUIHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC)
      Timber.tag(tag).v("Stop updating desktop lyrics")
      return super.cancel()
    }

    fun cancelByNotification() {
      needShowDesktopLyric = true
      showDesktopLyric = false
      updateUIHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC)
      cancel()
    }
  }


  private fun createDesktopLyric() {
    if (checkNoPermission()) {
      return
    }
    if (isDesktopLyricInitializing) {
      return
    }
    isDesktopLyricInitializing = true

    val param = WindowManager.LayoutParams()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      param.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      param.type = WindowManager.LayoutParams.TYPE_PHONE
    }

    param.format = PixelFormat.RGBA_8888
    param.gravity = Gravity.TOP
    param.width = resources.displayMetrics.widthPixels
    param.height = ViewGroup.LayoutParams.WRAP_CONTENT
    param.x = 0
    param.y = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_Y, 0)

    if (desktopLyricView != null) {
      windowManager.removeView(desktopLyricView)
      desktopLyricView = null
    }

    desktopLyricView = DesktopLyricView(service)
    windowManager.addView(desktopLyricView, param)
    isDesktopLyricInitializing = false
    Timber.v("Create desktop lyrics")
  }

  /**
   * 移除桌面歌词
   */
  private fun removeDesktopLyric() {
    if (desktopLyricView != null) {
      Timber.v("Remove desktop lyrics")
      //      desktopLyricView.cancelNotify();
      windowManager.removeView(desktopLyricView)
      desktopLyricView = null
    }
  }

  /**
   * 关闭桌面歌词
   */
  private fun closeDesktopLyric() {
    SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW, false)
    showDesktopLyric = false
    stopUpdateLyric()
    updateUIHandler.removeMessages(CREATE_DESKTOP_LRC)
    updateUIHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC)
  }

  private fun startSaveProgress() {
    if (progressTask != null) {
      return
    }
    progressTask = ProgressTask()
    timer.schedule(progressTask, 1000, LYRIC_FIND_INTERVAL)
  }

  private fun stopSaveProgress() {
    progressTask?.cancel()
    progressTask = null
  }


  /**
   * 保存当前播放的进度
   */
  private inner class ProgressTask : TimerTask() {
    override fun run() {
      val progress = progress
      if (progress > 0) {
        SPUtil.putValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_PLAY_PROGRESS, progress)
      }
    }

  }


  private inner class AudioFocusChangeListener : AudioManager.OnAudioFocusChangeListener {

    //记录焦点变化之前是否在播放;
    private var mNeedContinue = false

    override fun onAudioFocusChange(focusChange: Int) {
      when (focusChange) {
        //获得AudioFocus
        AudioManager.AUDIOFOCUS_GAIN -> {
          audioFocus = true
          if (!prepared) {
            setUpPlayer()
          } else if (mNeedContinue) {
            play(true)
            mNeedContinue = false
            operation = Command.TOGGLE
          }
          volumeController.directTo(1f)
        }
        //短暂暂停
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
          mNeedContinue = isPlay
          if (isPlay && prepared) {
            operation = Command.TOGGLE
            pause(false)
          }
        }
        //减小音量
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
          volumeController.directTo(.1f)
        }
        //暂停
        AudioManager.AUDIOFOCUS_LOSS -> {
          audioFocus = false
          if (isPlay && prepared) {
            operation = Command.TOGGLE
            pause(false)
          }
        }
      }
      //通知更新ui
//      updateUIHandler.sendEmptyMessage(Constants.UPDATE_UI);
//      updateUIHandler.sendEmptyMessage(Constants.UPDATE_META_DATA);
    }
  }


  private class PlaybackHandler internal constructor(looper: Looper)
    : Handler(looper) {

    override fun handleMessage(msg: Message) {
      super.handleMessage(msg)
    }
  }

  private class UpdateUIHandler internal constructor(
      service: MusicService,
      private val ref: WeakReference<MusicService> = WeakReference(service))
    : Handler() {

    override fun handleMessage(msg: Message) {
      if (ref.get() == null) {
        return
      }
      val musicService = ref.get() ?: return
      when (msg.what) {
        UPDATE_PLAY_STATE -> musicService.handlePlayStateChange()
        UPDATE_META_DATA -> {
//          musicService.handlePlayStateChange()
          musicService.handleMetaChange()
        }
        UPDATE_DESKTOP_LRC_CONTENT -> {
          if (msg.obj is LyricRowWrapper) {
            val wrapper = msg.obj as LyricRowWrapper
            musicService.desktopLyricView?.setText(wrapper.lineOne, wrapper.lineTwo)
          }
        }
        REMOVE_DESKTOP_LRC -> {
          musicService.removeDesktopLyric()
        }
        CREATE_DESKTOP_LRC -> {
          musicService.createDesktopLyric()
        }
      }
    }
  }

  private inner class ScreenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
      val action = intent.action
      Timber.tag("ScreenReceiver").v(action)
      if (Intent.ACTION_SCREEN_ON == action) {
        screenOn = true
        //显示锁屏
        if (isPlay && SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCKSCREEN, CASSETTE_LOCKSCREEN) == CASSETTE_LOCKSCREEN) {
          context.startActivity(Intent(context, LockScreenActivity::class.java)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        //重新显示桌面歌词
        updateDesktopLyric(false)
        //重新开始更新桌面部件
        updateAppwidget()
      } else {
        screenOn = false
        //停止更新桌面部件
        stopUpdateAppWidget()
        //关闭桌面歌词
        stopUpdateLyric()
      }
    }
  }

  companion object {

    const val TAG_LIFECYCLE = "ServiceLifeCycle"
    const val EXTRA_DESKTOP_LYRIC = "DesktopLyric"
    const val EXTRA_SONG = "Song"
    const val EXTRA_POSITION = "Position"

    //更新桌面部件
    const val UPDATE_APPWIDGET = 1000
    //更新正在播放歌曲
    const val UPDATE_META_DATA = 1002
    //更新播放状态
    const val UPDATE_PLAY_STATE = 1003
    //更新桌面歌词内容
    const val UPDATE_DESKTOP_LRC_CONTENT = 1004
    //移除桌面歌词
    const val REMOVE_DESKTOP_LRC = 1005
    //添加桌面歌词
    const val CREATE_DESKTOP_LRC = 1006

    private const val APLAYER_PACKAGE_NAME = "tanzent.cassette"
    //媒体数据库变化
    const val MEDIA_STORE_CHANGE = "$APLAYER_PACKAGE_NAME.media_store.change"
    //读写权限变化
    const val PERMISSION_CHANGE = "$APLAYER_PACKAGE_NAME.permission.change"
    //播放列表变换
    const val PLAYLIST_CHANGE = "$APLAYER_PACKAGE_NAME.playlist.change"
    //播放数据变化
    const val META_CHANGE = "$APLAYER_PACKAGE_NAME.meta.change"
    //播放状态变化
    const val PLAY_STATE_CHANGE = "$APLAYER_PACKAGE_NAME.play_state.change"

    const val EXTRA_CONTROL = "Control"
    const val ACTION_APPWIDGET_OPERATE = "$APLAYER_PACKAGE_NAME.appwidget.operate"
    const val ACTION_SHORTCUT_SHUFFLE = "$APLAYER_PACKAGE_NAME.shortcut.shuffle"
    const val ACTION_SHORTCUT_MYLOVE = "$APLAYER_PACKAGE_NAME.shortcut.my_love"
    const val ACTION_SHORTCUT_LASTADDED = "$APLAYER_PACKAGE_NAME.shortcut.last_added"
    const val ACTION_SHORTCUT_CONTINUE_PLAY = "$APLAYER_PACKAGE_NAME.shortcut.continue_play"
    const val ACTION_LOAD_FINISH = "$APLAYER_PACKAGE_NAME.load.finish"
    const val ACTION_CMD = "$APLAYER_PACKAGE_NAME.cmd"
    const val ACTION_WIDGET_UPDATE = "$APLAYER_PACKAGE_NAME.widget_update"
    const val ACTION_TOGGLE_TIMER = "$APLAYER_PACKAGE_NAME.toggle_timer"

    private const val MEDIA_SESSION_ACTIONS = (PlaybackStateCompat.ACTION_PLAY
        or PlaybackStateCompat.ACTION_PAUSE
        or PlaybackStateCompat.ACTION_PLAY_PAUSE
        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        or PlaybackStateCompat.ACTION_STOP
        or PlaybackStateCompat.ACTION_SEEK_TO)

    private const val APPWIDGET_BIG = "AppWidgetBig"
    private const val APPWIDGET_MEDIUM = "AppWidgetMedium"
    private const val APPWIDGET_SMALL = "AppWidgetSmall"
    private const val APPWIDGET_MEDIUM_TRANSPARENT = "AppWidgetMediumTransparent"
    private const val APPWIDGET_SMALL_TRANSPARENT = "AppWidgetSmallTransparent"


    /**
     * 复制bitmap
     */
    @JvmStatic
    fun copy(bitmap: Bitmap?): Bitmap? {
      if (bitmap == null || bitmap.isRecycled) {
        return null
      }
      var config: Bitmap.Config? = bitmap.config
      if (config == null) {
        config = Bitmap.Config.RGB_565
      }
      return try {
        bitmap.copy(config, false)
      } catch (e: OutOfMemoryError) {
        e.printStackTrace()
        null
      }

    }
  }
}

