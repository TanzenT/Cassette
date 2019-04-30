package tanzent.cassette.db.room.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Created by tanzent on 2019/1/12
 */
@Entity
data class History(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val audio_id: Int,
    val play_count: Int,
    val last_play: Long
) {
}