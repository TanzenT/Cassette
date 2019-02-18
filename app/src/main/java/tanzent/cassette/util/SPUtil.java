package tanzent.cassette.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tanzent.cassette.bean.misc.LyricPriority;

/**
 * Created by taeja on 16-1-15.
 */

/**
 * SharedPrefs工具类
 */
public class SPUtil {
    public static SPUtil mInstance;

    public SPUtil() {
        if (mInstance == null)
            mInstance = this;
    }

    public static boolean putStringSet(Context context, String name, String key, Set<String> set) {
        if (set == null)
            return false;
        SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit();
        editor.remove(key);
        return editor.putStringSet(key, set).commit();
    }

    public static Set<String> getStringSet(Context context, String name, String key) {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE).getStringSet(key, new HashSet<>());
    }

    public static boolean putValue(Context context, String name, String key, int value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit();
        return editor.putInt(key, value).commit();
    }

    public static boolean putValue(Context context, String name, String key, String value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit();
        return editor.putString(key, value).commit();
    }

    public static boolean putValue(Context context, String name, String key, boolean value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit();

        return editor.putBoolean(key, value).commit();
    }

    public static boolean getValue(Context context, String name, Object key, boolean dft) {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE).getBoolean(key.toString(), dft);
    }

    public static int getValue(Context context, String name, Object key, int dft) {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE).getInt(key.toString(), dft);
    }

    public static String getValue(Context context, String name, Object key, String dft) {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE).getString(key.toString(), dft);
    }

    public static void deleteValue(Context context, String name, String key) {
        SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit();
        editor.remove(key).apply();
    }

    public static void deleteFile(Context context, String name) {
        SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit();
        editor.clear().apply();
    }

    public interface UPDATE_KEY {
        String NAME = "Update";
    }

    public interface LYRIC_KEY {
        String NAME = "Lyric";
        //歌词搜索优先级
        String PRIORITY_LYRIC = "priority_lyric";
        String DEFAULT_PRIORITY = new Gson().toJson(Arrays.asList(LyricPriority.NETEASE, LyricPriority.KUGOU, LyricPriority.LOCAL, LyricPriority.EMBEDED),
                new TypeToken<List<LyricPriority>>() {
                }.getType());

        int LYRIC_DEFAULT = LyricPriority.DEF.getPriority();
        int LYRIC_IGNORE = LyricPriority.IGNORE.getPriority();
        int LYRIC_NETEASE = LyricPriority.NETEASE.getPriority();
        int LYRIC_KUGOU = LyricPriority.KUGOU.getPriority();
        int LYRIC_LOCAL = LyricPriority.LOCAL.getPriority();
        int LYRIC_EMBEDDED = LyricPriority.EMBEDED.getPriority();
        int LYRIC_MANUAL = LyricPriority.MANUAL.getPriority();

    }

    public interface COVER_KEY {
        String NAME = "Cover";
    }

    public interface SETTING_KEY {
        String NAME = "Setting";
        //桌面歌词是否可移动
        String FLOAT_LYRIC_LOCK = "float_lyric_lock";
        //桌面歌词字体大小
        String FLOAT_TEXT_SIZE = "float_text_size";
        //桌面歌词y坐标
        String FLOAT_Y = "float_y";
        //桌面歌词的字体颜色
        String FLOAT_TEXT_COLOR = "float_text_color";
        //是否开启屏幕常亮
        String SCREEN_ALWAYS_ON = "key_screen_always_on";
        //通知栏是否启用经典样式
        String NOTIFY_STYLE_CLASSIC = "notify_classic";
        //是否自动下载专辑封面
        String AUTO_DOWNLOAD_ALBUM_COVER = "auto_download_album_cover";
        //是否自动下载艺术家封面
        String AUTO_DOWNLOAD_ARTIST_COVER = "auto_download_artist_cover";
        //曲库配置
        String LIBRARY_CATEGORY = "library_category";
        //锁屏设置
        String LOCKSCREEN = "lockScreen";
        //导航浪变色
        String COLOR_NAVIGATION = "color_Navigation";
        //摇一摇
        String SHAKE = "shake";
        //优先搜索在线歌词
        String ONLINE_LYRIC_FIRST = "online_lyric_first";
        //是否开启桌面歌词
        String FLOAT_LYRIC_SHOW = "float_lyric_show";
        //沉浸式状态栏
        String IMMERSIVE_MODE = "immersive_mode";
        //过滤大小
        String SCAN_SIZE = "scan_size";
        //歌曲排序顺序
        String SONG_SORT_ORDER = "song_sort_order";
        //专辑排序顺序
        String ALBUM_SORT_ORDER = "album_sort_order";
        //艺术家排序顺序
        String ARTIST_SORT_ORDER = "artist_sort_order";
        //播放列表排序顺序
        String PLAYLIST_SORT_ORDER = "playlist_sort_order";
        //文件夹内歌曲排序顺序
        String CHILD_FOLDER_SONG_SORT_ORDER = "child_folder_song_sort_order";
        //艺术家内歌曲排序顺序
        String CHILD_ARTIST_SONG_SORT_ORDER = "child_artist_sort_order";
        //专辑内歌曲排序顺序
        String CHILD_ALBUM_SONG_SORT_ORDER = "child_album_song_sort_order";
        //播放列表内歌曲排序顺序
        String CHILD_PLAYLIST_SONG_SORT_ORDER = "child_playlist_song_sort_order";
        //移除歌曲
        String BLACKLIST_SONG = "black_list_song";
        //本地歌词搜索路径
        String LOCAL_LYRIC_SEARCH_DIR = "local_lyric_search_dir";
        //退出时播放时间
        String LAST_PLAY_PROGRESS = "last_play_progress";
        //退出时播放的歌曲
        String LAST_SONG_ID = "last_song_id";
        //退出时下一首歌曲
        String NEXT_SONG_ID = "next_song_id";
        //播放模式
        String PLAY_MODEL = "play_model";
        //经典通知栏背景是否是系统背景色
        String NOTIFY_SYSTEM_COLOR = "notify_system_color";
        //断点播放
        String PLAY_AT_BREAKPOINT = "play_at_breakpoint";
        //是否忽略媒体缓存
        String IGNORE_MEDIA_STORE = "ignore_media_store";
        //桌面部件样式
        String APP_WIDGET_SKIN = "app_widget_transparent";
        //是否默认开启定时器
        String TIMER_DEFAULT = "timer_default";
        //定时器时长
        String TIMER_DURATION = "timer_duration";
        //封面下载源
        String ALBUM_COVER_DOWNLOAD_SOURCE = "album_cover_download_source";
        //播放界面底部显示
        String BOTTOM_OF_NOW_PLAYING_SCREEN = "bottom_of_now_playing_screen";
        //倍速播放
        String SPEED = "speed";
        //移除是否同时源文件
        String DELETE_SOURCE = "delete_source";
        //是否保存日志文件到sd卡
        String WRITE_LOG_TO_STORAGE = "write_log_to_storage";
        //是否第一次显示多选
        String FIRST_SHOW_MULTI = "first_show_multi";
    }

    public interface LYRIC_OFFSET_KEY {
        String NAME = "LyricOffset";
    }
}
