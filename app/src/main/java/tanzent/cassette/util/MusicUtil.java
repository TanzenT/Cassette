package tanzent.cassette.util;

import static tanzent.cassette.helper.MusicServiceRemote.setPlayQueue;
import static tanzent.cassette.service.MusicService.EXTRA_POSITION;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import tanzent.cassette.App;
import tanzent.cassette.service.Command;
import tanzent.cassette.service.MusicService;
import timber.log.Timber;

public class MusicUtil {

  private static final String TAG = MusicUtil.class.getSimpleName();

  private MusicUtil() {
  }

  public static void playFromUri(Uri uri) {
    List<Integer> songs = null;
    if (uri.getScheme() != null && uri.getAuthority() != null) {
      if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
        String songId = null;
        switch (uri.getAuthority()) {
          case "com.android.providers.media.documents":
            songId = getSongIdFromMediaProvider(uri);
            break;
          case "com.android.browser.fileprovider":
            String fileProviderPath = uri.getPath();
            if (fileProviderPath != null && !TextUtils.isEmpty(fileProviderPath)) {
              songs = new ArrayList<>();
              if (fileProviderPath.startsWith("///")) {
                fileProviderPath = fileProviderPath.substring(2, fileProviderPath.length());
              }
              songs.add(MediaStoreUtil.getSongIdByUrl(fileProviderPath));
            }
            break;
          case "media":
            songId = uri.getLastPathSegment();
            break;
          default:
            String displayName = uri.getLastPathSegment();
            if (!TextUtils.isEmpty(displayName)) {
              songs = MediaStoreUtil
                  .getSongIds(MediaStore.Audio.Media.DISPLAY_NAME + "=?", new String[]{displayName},
                      null);
            }
            break;
        }
        if (songId != null && TextUtils.isDigitsOnly(songId)) {
          songs = new ArrayList<>();
          songs.add(Integer.valueOf(songId));
        }
      }
    }

    if (songs == null) {
      File songFile = null;
      if (uri.getAuthority() != null && uri.getAuthority()
          .equals("com.android.externalstorage.documents")) {
        songFile = new File(Environment.getExternalStorageDirectory(),
            uri.getPath().split(":", 2)[1]);
      }
      if (songFile == null) {
        String path = getFilePathFromUri(App.getContext(), uri);
        if (path != null) {
          songFile = new File(path);
        }
      }
      if (songFile == null && uri.getPath() != null) {
        songFile = new File(uri.getPath());
      }
      if (songFile != null) {
        int id = MediaStoreUtil.getSongIdByUrl(songFile.getAbsolutePath());
        if (id > 0) {
          songs = new ArrayList<>();
          songs.add(id);
        }
      }
    }

    if (songs != null && !songs.isEmpty()) {
      setPlayQueue(songs, makeCmdIntent(Command.PLAYSELECTEDSONG)
          .putExtra(EXTRA_POSITION, 0));
    } else {
      Timber.v("unknown uri");
    }
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  private static String getSongIdFromMediaProvider(Uri uri) {
    return DocumentsContract.getDocumentId(uri).split(":")[1];
  }

  @Nullable
  private static String getFilePathFromUri(Context context, Uri uri) {
    final String column = "_data";
    final String[] projection = {
        column
    };

    try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null,
        null)) {
      if (cursor != null && cursor.moveToFirst()) {
        final int column_index = cursor.getColumnIndex(column);
        return cursor.getString(column_index);
      }
    } catch (Exception e) {
      Timber.v(e);
    }
    return null;
  }

  public static Intent makeCmdIntent(int cmd, boolean shuffle) {
    return new Intent(MusicService.ACTION_CMD).putExtra("Control", cmd)
        .putExtra("shuffle", shuffle);
  }

  public static Intent makeCmdIntent(int cmd) {
    return makeCmdIntent(cmd, false);
  }
}
