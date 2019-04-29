package tanzent.cassette.misc.asynctask;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;
import tanzent.cassette.App;
import tanzent.cassette.util.Constants;
import tanzent.cassette.util.MediaStoreUtil;
import tanzent.cassette.util.Util;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/11 14:07
 */
@Deprecated
public abstract class AsynLoadSongNum extends AsyncTask<Integer, Integer, Integer> {

  private final int mType;

  public AsynLoadSongNum(int type) {
    mType = type;
  }

  @Override
  protected Integer doInBackground(Integer... params) {
    ContentResolver resolver = App.getContext().getContentResolver();
    boolean isAlbum = mType == Constants.ALBUM;
    Cursor cursor = null;
    try {
      cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
          new String[]{MediaStore.Audio.Media._ID},
          MediaStoreUtil.getBaseSelection()
              + " and " + (isAlbum ? MediaStore.Audio.Media.ALBUM_ID
              : MediaStore.Audio.Media.ARTIST_ID) + "=" + params[0],
          null, null);
      return cursor != null ? cursor.getCount() : 0;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      Util.closeSafely(cursor);
    }
    return 0;
  }

}
