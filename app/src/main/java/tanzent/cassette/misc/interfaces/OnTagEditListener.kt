package tanzent.cassette.misc.interfaces

import tanzent.cassette.bean.mp3.Song

interface OnTagEditListener {
    fun onTagEdit(newSong: Song?)
}
