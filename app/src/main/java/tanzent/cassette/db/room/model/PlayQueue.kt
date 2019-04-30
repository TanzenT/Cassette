package tanzent.cassette.db.room.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

/**
 * Created by tanzent on 2019/1/12
 */
@Entity(indices = [Index(value = ["audio_id"], unique = true)])
data class PlayQueue(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val audio_id: Int
) {

  companion object {
    const val TABLE_NAME = "PlayQueue"
  }
}