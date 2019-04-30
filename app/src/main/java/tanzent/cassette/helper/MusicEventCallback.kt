package tanzent.cassette.helper

import tanzent.cassette.service.MusicService

interface MusicEventCallback {
  fun onMediaStoreChanged()

  fun onPermissionChanged(has: Boolean)

  fun onPlayListChanged(name: String)

  fun onServiceConnected(service: MusicService)

  fun onMetaChanged()

  fun onPlayStateChange()

  fun onServiceDisConnected()
}
