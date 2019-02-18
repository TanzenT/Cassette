package tanzent.cassette.misc.tageditor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import tanzent.cassette.bean.mp3.Song;
import tanzent.cassette.misc.interfaces.OnTagEditListener;

public class TagReceiver extends BroadcastReceiver {
    private final OnTagEditListener mListener;

    public TagReceiver(OnTagEditListener listener) {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Song newSong = intent.getParcelableExtra("newSong");
        if (mListener != null)
            mListener.onTagEdit(newSong);
    }
}
