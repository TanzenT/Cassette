package tanzent.cassette.util;

import android.os.Environment;
import android.text.TextUtils;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import tanzent.cassette.lyric.SearchLrc;

public class LyricUtil {
    private static final String TAG = "LyricUtil";

    private LyricUtil() {
    }


    /**
     * 查找歌曲的lrc文件
     *
     * @param songName
     * @param searchPath
     */
    public static void searchFile(String displayName, String songName, String artistName, File searchPath) {
        //判断SD卡是否存在
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File[] files = searchPath.listFiles();
            if (files == null || files.length == 0)
                return;
            for (File file : files) {
                if (file.isDirectory() && file.canRead()) {
                    searchFile(displayName, songName, artistName, file);
                } else {
                    if (isRightLrc(file, displayName, songName, artistName)) {
                        SearchLrc.setLOCAL_LYRIC_PATH(file.getAbsolutePath());
                    }
                }
            }
        }

    }

    /**
     * 判断是否是相匹配的歌词
     *
     * @param file
     * @param title
     * @param artist
     * @return
     */
    public static boolean isRightLrc(File file, String displayName, String title, String artist) {
        BufferedReader br = null;
        try {
            if (file == null || !file.canRead() || !file.isFile())
                return false;
            if (TextUtils.isEmpty(file.getAbsolutePath()) || TextUtils.isEmpty(displayName) ||
                    TextUtils.isEmpty(title) || TextUtils.isEmpty(artist))
                return false;
            //仅判断.lrc文件
            if (!file.getName().endsWith("lrc"))
                return false;
            //暂时忽略网易云的歌词
            if (file.getAbsolutePath().contains("netease/cloudmusic/"))
                return false;
            String fileName = file.getName().indexOf('.') > 0 ?
                    file.getName().substring(0, file.getName().lastIndexOf('.')) : file.getName();
            //判断歌词文件名与歌曲文件名是否一致
            if (fileName.equalsIgnoreCase(displayName)) {
                return true;
            }
            //判断是否包含歌手名和歌曲名
            if (fileName.toUpperCase().contains(title.toUpperCase()) && fileName.toUpperCase().contains(artist.toUpperCase())) {
                return true;
            }
            //读取前五行歌词内容进行判断
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), getCharset(file.getAbsolutePath())));
            boolean hasArtist = false;
            boolean hasTitle = false;
            for (int i = 0; i < 5; i++) {
                String lrcLine;
                if ((lrcLine = br.readLine()) == null)
                    break;
                if (lrcLine.contains("ar") && lrcLine.equalsIgnoreCase(artist)) {
                    hasArtist = true;
                    continue;
                }
                if (lrcLine.contains("ti") && lrcLine.equalsIgnoreCase(title)) {
                    hasTitle = true;
                }
            }
            if (hasArtist && hasTitle) {
                return true;
            }
        } catch (Exception e) {

        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static String getCharset(final String filePath) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(filePath);
            byte[] buf = new byte[1024];
            UniversalDetector detector = new UniversalDetector(null);
            int hasRead;
            while ((hasRead = fileInputStream.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, hasRead);
            }
            detector.dataEnd();
            String encoding = detector.getDetectedCharset();
            detector.reset();
            fileInputStream.close();
            return !TextUtils.isEmpty(encoding) ? encoding : "UTF-8";
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return "UTF-8";
    }
}
