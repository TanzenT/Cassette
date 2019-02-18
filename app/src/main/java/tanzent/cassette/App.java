package tanzent.cassette;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.multidex.MultiDexApplication;

import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipelineConfig;

import io.reactivex.plugins.RxJavaPlugins;
import tanzent.cassette.appshortcuts.DynamicShortcutManager;
import tanzent.cassette.db.DBManager;
import tanzent.cassette.db.DBOpenHelper;
import tanzent.cassette.misc.cache.DiskCache;
import tanzent.cassette.misc.exception.RxException;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.util.CrashHandler;
import tanzent.cassette.util.LogUtil;
import tanzent.cassette.util.MediaStoreUtil;
import tanzent.cassette.util.PlayListUtil;
import tanzent.cassette.util.SPUtil;
import tanzent.cassette.util.Util;

public class App extends MultiDexApplication {
    private static Context mContext;

    public static boolean IS_GOOGLEPLAY;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();

        if (!BuildConfig.DEBUG)
            IS_GOOGLEPLAY = "google".equalsIgnoreCase(Util.getAppMetaData("UMENG_CHANNEL"));
        initUtil();
        initTheme();

        CrashHandler.getInstance().init(this);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
            new DynamicShortcutManager(this).setUpShortcut();

        loadLibrary();

        RxJavaPlugins.setErrorHandler(throwable -> {
            LogUtil.e("RxError", throwable);
        });

        if (SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, "CategoryReset", true)) {
            SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, "CategoryReset", false);
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY, "");
        }
    }

    private void initUtil() {
        DBManager.initialInstance(new DBOpenHelper(this));
        DiskCache.init(this);
        MediaStoreUtil.setContext(this);
        PlayListUtil.setContext(this);
    }

    private void initTheme() {
        ThemeStore.THEME_MODE = ThemeStore.loadThemeMode();
        ThemeStore.THEME_COLOR = ThemeStore.loadThemeColor();

        ThemeStore.MATERIAL_COLOR_PRIMARY = ThemeStore.getMaterialPrimaryColorRes();
        ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = ThemeStore.getMaterialPrimaryDarkColorRes();
    }

    public static Context getContext() {
        return mContext;
    }

    private void loadLibrary() {
        final int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 8);
        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setBitmapMemoryCacheParamsSupplier(() -> new MemoryCacheParams(cacheSize, Integer.MAX_VALUE, cacheSize, Integer.MAX_VALUE, 2 * ByteConstants.MB))
                .setBitmapsConfig(Bitmap.Config.RGB_565)
                .setDownsampleEnabled(true)
                .build();
        Fresco.initialize(this, config);
    }
}
