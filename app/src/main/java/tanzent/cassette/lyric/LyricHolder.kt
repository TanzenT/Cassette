package tanzent.cassette.lyric

import io.reactivex.disposables.Disposable
import tanzent.cassette.App
import tanzent.cassette.bean.mp3.Song
import tanzent.cassette.lyric.bean.LrcRow
import tanzent.cassette.lyric.bean.LrcRow.LYRIC_EMPTY_ROW
import tanzent.cassette.lyric.bean.LyricRowWrapper
import tanzent.cassette.lyric.bean.LyricRowWrapper.LYRIC_WRAPPER_NO
import tanzent.cassette.lyric.bean.LyricRowWrapper.LYRIC_WRAPPER_SEARCHING
import tanzent.cassette.service.MusicService
import tanzent.cassette.util.SPUtil
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Created by tanzent on 2019/2/6
 */
class LyricHolder(service: MusicService) {

  @Volatile
  private var lrcRows: List<LrcRow>? = null
  private val reference: WeakReference<MusicService> = WeakReference(service)
  private var disposable: Disposable? = null
  private var song: Song = Song.EMPTY_SONG
  private var status = Status.SEARCHING
  var offset = 0
  private val lyricSearcher = LyricSearcher()


  fun findCurrentLyric(): LyricRowWrapper {
    val wrapper = LyricRowWrapper()
    wrapper.status = status
    val service = reference.get()

    when {
      service == null || status == Status.NO -> {
        return LYRIC_WRAPPER_NO
      }
      status == Status.SEARCHING -> {
        return LYRIC_WRAPPER_SEARCHING
      }
      status == Status.NORMAL -> {
        val song = service.currentSong
        if (song == Song.EMPTY_SONG) {
          Timber.v("Unusual song")
          return wrapper
        }
        val progress = service.progress + offset

        lrcRows?.let { lrcRows ->
          for (i in lrcRows.indices.reversed()) {
            val lrcRow = lrcRows[i]
            val interval = progress - lrcRow.time
            if (i == 0 && interval < 0) {
              //未开始歌唱前显示歌曲信息
              wrapper.lineOne = LrcRow("", 0, song.title)
              wrapper.lineTwo = LrcRow("", 0, song.artist + " - " + song.album)
              return wrapper
            } else if (progress >= lrcRow.time) {
              /*if (lrcRow.hasTranslate()) {
                wrapper.lineOne = LrcRow(lrcRow)
                wrapper.lineOne.content = lrcRow.content
                wrapper.lineTwo = LrcRow(lrcRow)
                wrapper.lineTwo.content = lrcRow.translate
              } else {*/
                wrapper.lineOne = lrcRow
                wrapper.lineTwo = LrcRow(if (i + 1 < lrcRows.size) lrcRows[i + 1] else LYRIC_EMPTY_ROW)
              /*}*/
              return wrapper
            }
          }

        }
        return wrapper

      }

      else -> {
        return LYRIC_WRAPPER_NO
      }
    }

  }


  fun updateLyricRows(song: Song) {
    this.song = song

    if (song == Song.EMPTY_SONG) {
      status = Status.NO
      lrcRows = null
      return
    }

    val id = song.id

    disposable?.dispose()
    disposable = lyricSearcher.setSong(song)
        .getLyricObservable()
        .doOnSubscribe { status = Status.SEARCHING }
        .subscribe({ it ->
          if (id == song.id) {
            status = Status.NORMAL
            offset = SPUtil.getValue(App.getContext(), SPUtil.LYRIC_OFFSET_KEY.NAME, id.toString(), 0)
            lrcRows = it
          }
        }, { throwable ->
          Timber.v(throwable)
          if (id == song.id) {
            status = Status.NO
            lrcRows = null
          }
        })
  }

  fun dispose() {
    disposable?.dispose()
  }

  enum class Status {
    NO, SEARCHING, NORMAL
  }

  companion object {
    const val LYRIC_FIND_INTERVAL = 400L
  }

}