package tanzent.cassette.db.room.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update
import tanzent.cassette.db.room.model.History


/**
 * Created by tanzent on 2019/1/12
 */
@Dao
interface HistoryDao {
  @Insert(onConflict = REPLACE)
  fun insertHistory(histories: List<History>): LongArray

  @Insert(onConflict = REPLACE)
  fun insertHistory(history: History): Long

  @Query("""
    SELECT * FROM History
  """)
  fun selectAll(): List<History>

  @Query("""
    SELECT * FROM History
    WHERE audio_id = :audioId
  """)
  fun selectByAudioId(audioId: Int): History?

  @Update
  fun update(history: History): Int
}