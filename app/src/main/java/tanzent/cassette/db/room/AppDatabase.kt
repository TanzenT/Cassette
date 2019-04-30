package tanzent.cassette.db.room

import android.arch.persistence.room.Database
import android.arch.persistence.room.InvalidationTracker
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import android.content.Intent
import tanzent.cassette.db.room.AppDatabase.Companion.VERSION
import tanzent.cassette.db.room.dao.HistoryDao
import tanzent.cassette.db.room.dao.PlayListDao
import tanzent.cassette.db.room.dao.PlayQueueDao
import tanzent.cassette.db.room.model.History
import tanzent.cassette.db.room.model.PlayList
import tanzent.cassette.db.room.model.PlayQueue
import tanzent.cassette.service.MusicService
import tanzent.cassette.ui.activity.base.BaseMusicActivity.Companion.EXTRA_PLAYLIST
import tanzent.cassette.util.Util.sendLocalBroadcast
import timber.log.Timber

/**
 * Created by tanzent on 2019/1/12
 */
@Database(entities = [
  PlayList::class,
  PlayQueue::class,
  History::class
], version = VERSION, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

  abstract fun playListDao(): PlayListDao

  abstract fun playQueueDao(): PlayQueueDao

  abstract fun historyDao(): HistoryDao

  companion object {
    const val VERSION = 1

    @Volatile
    private var INSTANCE: AppDatabase? = null

    @JvmStatic
    fun getInstance(context: Context): AppDatabase =
        INSTANCE ?: synchronized(this) {
          INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

    private fun buildDatabase(context: Context): AppDatabase {
      val database = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "Cassette.db")
          .build()
      database.invalidationTracker.addObserver(object : InvalidationTracker.Observer(PlayList.TABLE_NAME,PlayQueue.TABLE_NAME) {
        override fun onInvalidated(tables: MutableSet<String>) {
          Timber.v("onInvalidated: $tables")
          if(tables.contains(PlayList.TABLE_NAME)){
            sendLocalBroadcast(Intent(MusicService.PLAYLIST_CHANGE)
                .putExtra(EXTRA_PLAYLIST,PlayList.TABLE_NAME))
          } else if(tables.contains(PlayQueue.TABLE_NAME)){
            sendLocalBroadcast(Intent(MusicService.PLAYLIST_CHANGE)
                .putExtra(EXTRA_PLAYLIST,PlayQueue.TABLE_NAME))
          }
        }
      })
      return database
    }

  }
}
