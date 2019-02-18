package tanzent.cassette.misc.update

import tanzent.cassette.bean.github.Release

interface Listener {
    fun onUpdateReturned(code: Int, message: String, release: Release?)
    fun onUpdateError(throwable: Throwable)
}
