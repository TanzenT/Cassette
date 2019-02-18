package tanzent.cassette.misc.tageditor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.support.annotation.Nullable;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

import io.reactivex.Observable;
import tanzent.cassette.App;
import tanzent.cassette.bean.mp3.Song;
import tanzent.cassette.util.MediaStoreUtil;

/**
 * 标签相关
 */
public class TagEditor {
    private final AudioFile mAudioFile;
    private final String mPath;
    private final AudioHeader mAudioHeader;

    public TagEditor(String path) {
        mPath = path;
        mAudioFile = getAudioFile();
        mAudioHeader = mAudioFile.getAudioHeader();
    }

    @NotNull
    public AudioFile getAudioFile() {
        try {
            return AudioFileIO.read(new File(mPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new AudioFile();
    }

    public String getFormat() {
        return mAudioHeader != null ? mAudioHeader.getFormat() : "";
    }

    public String getBitrate() {
        return mAudioHeader != null ? mAudioHeader.getBitRate() : "";
    }

    public String getSamplingRate() {
        return mAudioHeader != null ? mAudioHeader.getSampleRate() : "";
    }

    @Nullable
    public String getFiledValue(FieldKey field) {
        if (mAudioFile == null)
            return "";
        try {
            return mAudioFile.getTagOrCreateAndSetDefault().getFirst(field);
        } catch (Exception e) {
            return "";
        }
    }

    @Nullable
    public String getFiledValue(String path, String field) {
        if (mAudioFile == null)
            return "";
        try {
            return mAudioFile.getTagOrCreateAndSetDefault().getFirst(field);
        } catch (Exception e) {
            return "";
        }
    }

    @Nullable
    public String getSongTitle() {
        return getFiledValue(FieldKey.TITLE);
    }

    @Nullable
    public String getAlbumTitle() {
        return getFiledValue(FieldKey.ALBUM);
    }

    @Nullable
    public String getArtistName() {
        return getFiledValue(FieldKey.ARTIST);
    }

    @Nullable
    public String getAlbumArtistName() {
        return getFiledValue(FieldKey.ALBUM_ARTIST);
    }

    @Nullable
    public String getGenreName() {
        return getFiledValue(FieldKey.GENRE);
    }

    @Nullable
    public String getSongYear() {
        return getFiledValue(FieldKey.YEAR);
    }

    @Nullable
    public String getTrackNumber() {
        return getFiledValue(FieldKey.TRACK);
    }

    @Nullable
    public String getLyric() {
        return getFiledValue(FieldKey.LYRICS);
    }

    @Nullable
    public Bitmap getAlbumArt() {
        if (mAudioFile == null)
            return null;
        try {
            Artwork artworkTag = mAudioFile.getTagOrCreateAndSetDefault().getFirstArtwork();
            if (artworkTag != null) {
                byte[] artworkBinaryData = artworkTag.getBinaryData();
                return BitmapFactory.decodeByteArray(artworkBinaryData, 0, artworkBinaryData.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Observable<Song> save(Song song, String title, String album, String artist, String year, String genre, String trackNumber, String lyric) {
        return Observable.create(e -> {
            if (mAudioFile == null) {
                throw new IllegalArgumentException("AudioFile is null");
            }

            Map<FieldKey, String> fieldKeyValueMap = new EnumMap<>(FieldKey.class);
            fieldKeyValueMap.put(FieldKey.ALBUM, album);
            fieldKeyValueMap.put(FieldKey.TITLE, title);
            fieldKeyValueMap.put(FieldKey.YEAR, year);
            fieldKeyValueMap.put(FieldKey.GENRE, genre);
            fieldKeyValueMap.put(FieldKey.ARTIST, artist);
            fieldKeyValueMap.put(FieldKey.TRACK, trackNumber);
//            fieldKeyValueMap.put(FieldKey.LYRICS,lyric);

            Tag tag = mAudioFile.getTagOrCreateAndSetDefault();
            for (Map.Entry<FieldKey, String> entry : fieldKeyValueMap.entrySet()) {
                try {
                    tag.setField(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

//            tag.setField(FieldKey.ALBUM,album == null ? "" : album);
//            tag.setField(FieldKey.ARTIST,artist == null ? "" : artist);
//            tag.setField(FieldKey.YEAR,year == null ? "" : year);
//            tag.setField(FieldKey.GENRE,genre == null ? "" : genre);
//            tag.setField(FieldKey.TRACK,trackNumber == null ? "" : trackNumber);
//            tag.setField(FieldKey.LYRICS,lyric == null ? "" : lyric);

            mAudioFile.commit();

            MediaScannerConnection.scanFile(App.getContext(),
                    new String[]{song.getUrl()},
                    null,
                    (path, uri) -> {
                        App.getContext().getContentResolver().notifyChange(uri, null);
                        e.onNext(MediaStoreUtil.getSongById(song.getId()));
                        e.onComplete();
                    });

        });
    }
}
