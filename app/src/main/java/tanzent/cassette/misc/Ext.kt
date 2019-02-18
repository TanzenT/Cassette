package tanzent.cassette.misc

import android.provider.MediaStore
import tanzent.cassette.bean.mp3.Album
import tanzent.cassette.bean.mp3.Artist
import tanzent.cassette.bean.mp3.Folder
import tanzent.cassette.bean.mp3.PlayList
import tanzent.cassette.util.MediaStoreUtil
import tanzent.cassette.util.PlayListUtil

fun Album.getSongIds(): List<Int> {
    return MediaStoreUtil.getSongIds(MediaStore.Audio.Media.ALBUM_ID + "=?", arrayOf((albumID.toString())))
}

fun Artist.getSongIds(): List<Int> {
    return MediaStoreUtil.getSongIds(MediaStore.Audio.Media.ARTIST_ID + "=?", arrayOf(artistID.toString()))
}

fun Folder.getSongIds(): List<Int> {
    return MediaStoreUtil.getSongIdsByParentId(parentId)
}

fun PlayList.getSongIds(): List<Int> {
    return PlayListUtil.getSongIds(_Id)
}
