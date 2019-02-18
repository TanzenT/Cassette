package tanzent.cassette.helper;

import tanzent.cassette.service.MusicService;

public interface MusicEventCallback {
    void onMediaStoreChanged();

    void onPermissionChanged(boolean has);

    void onPlayListChanged();

    void onServiceConnected(MusicService service);

    void onMetaChanged();

    void onPlayStateChange();

    void onServiceDisConnected();
}
