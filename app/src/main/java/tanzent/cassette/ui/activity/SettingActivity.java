package tanzent.cassette.ui.activity;

import static tanzent.cassette.App.IS_GOOGLEPLAY;
import static tanzent.cassette.helper.LanguageHelper.AUTO;
import static tanzent.cassette.helper.M3UHelper.exportPlayListToFile;
import static tanzent.cassette.helper.M3UHelper.importLocalPlayList;
import static tanzent.cassette.helper.M3UHelper.importM3UFile;
import static tanzent.cassette.request.ImageUriRequest.DOWNLOAD_LASTFM;
import static tanzent.cassette.request.network.RxUtil.applySingleScheduler;
import static tanzent.cassette.service.MusicService.EXTRA_DESKTOP_LYRIC;
import static tanzent.cassette.theme.Theme.getBaseDialog;
import static tanzent.cassette.theme.ThemeStore.getThemeText;
import static tanzent.cassette.theme.ThemeStore.saveAccentColor;
import static tanzent.cassette.theme.ThemeStore.saveMaterialPrimaryColor;
import static tanzent.cassette.ui.activity.MainActivity.EXTRA_CATEGORY;
import static tanzent.cassette.ui.activity.MainActivity.EXTRA_RECREATE;
import static tanzent.cassette.ui.activity.MainActivity.EXTRA_REFRESH_ADAPTER;
import static tanzent.cassette.ui.activity.MainActivity.EXTRA_REFRESH_LIBRARY;
import static tanzent.cassette.util.SPUtil.SETTING_KEY.BOTTOM_OF_NOW_PLAYING_SCREEN;
import static tanzent.cassette.util.Util.sendLocalBroadcast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import tanzent.cassette.R;
import tanzent.cassette.bean.misc.Category;
import tanzent.cassette.bean.mp3.Song;
import tanzent.cassette.db.room.DatabaseRepository;
import tanzent.cassette.db.room.model.PlayList;
import tanzent.cassette.helper.LanguageHelper;
import tanzent.cassette.helper.MusicServiceRemote;
import tanzent.cassette.helper.ShakeDetector;
import tanzent.cassette.misc.MediaScanner;
import tanzent.cassette.misc.floatpermission.FloatWindowManager;
import tanzent.cassette.misc.handler.MsgHandler;
import tanzent.cassette.misc.handler.OnHandleMessage;
import tanzent.cassette.request.ImageUriRequest;
import tanzent.cassette.service.Command;
import tanzent.cassette.service.MusicService;
import tanzent.cassette.theme.Theme;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.theme.TintHelper;
import tanzent.cassette.ui.dialog.FileChooserDialog;
import tanzent.cassette.ui.dialog.FolderChooserDialog;
import tanzent.cassette.ui.dialog.LyricPriorityDialog;
import tanzent.cassette.ui.dialog.color.ColorChooserDialog;
import tanzent.cassette.util.Constants;
import tanzent.cassette.util.MediaStoreUtil;
import tanzent.cassette.util.MusicUtil;
import tanzent.cassette.util.SPUtil;
import tanzent.cassette.util.SPUtil.SETTING_KEY;
import tanzent.cassette.util.ToastUtil;
import tanzent.cassette.util.Util;

/**
 * @ClassName SettingActivity
 * @Description 设置界面
 * @Author Xiaoborui
 * @Date 2016/8/23 13:51
 */
public class SettingActivity extends ToolbarActivity implements FolderChooserDialog.FolderCallback,
    FileChooserDialog.FileCallback, ColorChooserDialog.ColorCallback {

  @BindView(R.id.setting_color_primary_indicator)
  ImageView mPrimaryColorSrc;
  @BindView(R.id.setting_color_accent_indicator)
  ImageView mAccentColorSrc;
  @BindView(R.id.setting_clear_text)
  TextView mCache;
  @BindView(R.id.setting_navaigation_switch)
  SwitchCompat mNaviSwitch;
  @BindView(R.id.setting_shake_switch)
  SwitchCompat mShakeSwitch;
  @BindView(R.id.setting_lrc_float_switch)
  SwitchCompat mFloatLrcSwitch;
  @BindView(R.id.setting_lrc_float_tip)
  TextView mFloatLrcTip;
  @BindView(R.id.setting_screen_switch)
  SwitchCompat mScreenSwitch;
  @BindView(R.id.setting_notify_switch)
  SwitchCompat mNotifyStyleSwitch;
  @BindView(R.id.setting_notify_color_container)
  View mNotifyColorContainer;
  @BindView(R.id.setting_album_cover_text)
  TextView mAlbumCoverText;
  @BindView(R.id.setting_lockscreen_text)
  TextView mLockScreenTip;
  @BindView(R.id.setting_immersive_switch)
  SwitchCompat mImmersiveSwitch;
  @BindView(R.id.setting_breakpoint_switch)
  SwitchCompat mBreakpointSwitch;
  @BindView(R.id.setting_ignore_mediastore_switch)
  SwitchCompat mIgnoreMediastoreSwitch;
  @BindView(R.id.setting_displayname_switch)
  SwitchCompat mShowDisplaynameSwitch;
  @BindView(R.id.setting_general_theme_text)
  TextView mThemeText;

  @BindViews({R.id.setting_common_title, R.id.setting_color_title, R.id.setting_cover_title,
      R.id.setting_library_title, R.id.setting_lrc_title, R.id.setting_notify_title,
      R.id.setting_other_title, R.id.setting_player_title})
  TextView[] mTitles;

  @BindViews({R.id.setting_eq_arrow, R.id.setting_feedback_arrow, R.id.setting_update_arrow})
  ImageView[] mArrows;

  private static final int REQUEST_THEME_COLOR = 0x10;
  private static final int REQUEST_EQ = 0x100;

  //是否需要重建activity
  private boolean mNeedRecreate = false;
  //是否需要刷新adapter
  private boolean mNeedRefreshAdapter = false;
  //是否需要刷新library
  private boolean mNeedRefreshLibrary;
  //    //是否从主题颜色选择对话框返回
//    private boolean mFromColorChoose = false;
  //缓存大小
  private long mCacheSize = 0;
  private final int RECREATE = 100;
  private final int CACHE_SIZE = 101;
  private final int CLEAR_FINISH = 102;
  private MsgHandler mHandler;
  private final int[] mScanSize = new int[]{0, 500 * ByteConstants.KB, ByteConstants.MB,
      2 * ByteConstants.MB};
  private String mOriginalAlbumChoice;

  private List<Disposable> mDisposables = new ArrayList<>();


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_setting);
    ButterKnife.bind(this);
    setUpToolbar(getString(R.string.setting));
    mHandler = new MsgHandler(this);
    //读取重启aitivity之前的数据
    if (savedInstanceState != null) {
      mNeedRecreate = savedInstanceState.getBoolean(EXTRA_RECREATE);
      mNeedRefreshAdapter = savedInstanceState.getBoolean(EXTRA_REFRESH_ADAPTER);
//            mFromColorChoose = savedInstanceState.getBoolean("fromColorChoose");
    }

    final String[] keyWord = new String[]{SPUtil.SETTING_KEY.COLOR_NAVIGATION,
        SPUtil.SETTING_KEY.SHAKE,
        SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW, SPUtil.SETTING_KEY.SCREEN_ALWAYS_ON,
        SPUtil.SETTING_KEY.NOTIFY_STYLE_CLASSIC, SPUtil.SETTING_KEY.IMMERSIVE_MODE,
        SPUtil.SETTING_KEY.PLAY_AT_BREAKPOINT, SPUtil.SETTING_KEY.IGNORE_MEDIA_STORE,
        SPUtil.SETTING_KEY.SHOW_DISPLAYNAME};
    ButterKnife.apply(new SwitchCompat[]{mNaviSwitch, mShakeSwitch, mFloatLrcSwitch,
        mScreenSwitch, mNotifyStyleSwitch, mImmersiveSwitch, mBreakpointSwitch,
        mIgnoreMediastoreSwitch, mShowDisplaynameSwitch}, new ButterKnife.Action<SwitchCompat>() {
      @Override
      public void apply(@NonNull SwitchCompat view, final int index) {
        TintHelper.setTintAuto(view, ThemeStore.getAccentColor(), false);

        view.setChecked(SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, keyWord[index], false));
        //5.0以上才支持变色导航栏
        if (view.getId() == R.id.setting_navaigation_switch) {
          view.setEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
        }
        view.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, keyWord[index], isChecked);
            switch (buttonView.getId()) {
              //变色导航栏
              case R.id.setting_navaigation_switch:
                mNeedRecreate = true;
                mHandler.sendEmptyMessage(RECREATE);
                break;
              //摇一摇
              case R.id.setting_shake_switch:
                if (isChecked) {
                  ShakeDetector.getInstance().beginListen();
                } else {
                  ShakeDetector.getInstance().stopListen();
                }
                break;
              //桌面歌词
              case R.id.setting_lrc_float_switch:
                if (isChecked && !FloatWindowManager.getInstance().checkPermission(mContext)) {
                  mFloatLrcSwitch.setOnCheckedChangeListener(null);
                  mFloatLrcSwitch.setChecked(false);
                  mFloatLrcSwitch.setOnCheckedChangeListener(this);
                  if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Util.startActivitySafely(mContext, intent);
                  }
                  ToastUtil.show(mContext, R.string.plz_give_float_permission);
                  return;
                }
                mFloatLrcTip
                    .setText(isChecked ? R.string.opened_desktop_lrc : R.string.closed_desktop_lrc);
                Intent intent = MusicUtil.makeCmdIntent(Command.TOGGLE_DESKTOP_LYRIC);
                intent.putExtra(EXTRA_DESKTOP_LYRIC, mFloatLrcSwitch.isChecked());
                sendLocalBroadcast(intent);
                break;
              //屏幕常亮
              case R.id.setting_screen_switch:
                break;
              //通知栏样式
              case R.id.setting_notify_switch:
                sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.TOGGLE_NOTIFY)
                    .putExtra(SPUtil.SETTING_KEY.NOTIFY_STYLE_CLASSIC, isChecked));
                break;
              //沉浸式状态栏
              case R.id.setting_immersive_switch:
                ThemeStore.IMMERSIVE_MODE = isChecked;
                mNeedRecreate = true;
                mHandler.sendEmptyMessage(RECREATE);
                break;
              //断点播放
              case R.id.setting_breakpoint_switch:
                sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.PLAY_AT_BREAKPOINT)
                    .putExtra(SPUtil.SETTING_KEY.PLAY_AT_BREAKPOINT, isChecked));
                break;
              //忽略内嵌
              case R.id.setting_ignore_mediastore_switch:
                ImageUriRequest.IGNORE_MEDIA_STORE = isChecked;
                mNeedRefreshAdapter = true;
                break;
              //文件名
              case R.id.setting_displayname_switch:
                Song.SHOW_DISPLAYNAME = isChecked;
                mNeedRefreshAdapter = true;
                break;
            }

          }
        });
      }
    });

//        //歌词搜索路径
//        if (!SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCAL_LYRIC_SEARCH_DIR, "").equals("")) {
//            mLrcPath.setText(getString(R.string.lrc_tip, SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCAL_LYRIC_SEARCH_DIR, "")));
//        }
    //桌面歌词
    mFloatLrcTip.setText(
        mFloatLrcSwitch.isChecked() ? R.string.opened_desktop_lrc : R.string.closed_desktop_lrc);

    //主题颜色指示器
    ((GradientDrawable) mPrimaryColorSrc.getDrawable())
        .setColor(ThemeStore.getMaterialPrimaryColor());
    ((GradientDrawable) mAccentColorSrc.getDrawable()).setColor(ThemeStore.getAccentColor());

    //初始化箭头颜色
    final int accentColor = ThemeStore.getAccentColor();
    ButterKnife.apply(mArrows,
        (view, index) -> Theme.tintDrawable(view, view.getBackground(), accentColor));

    //标题颜色
    ButterKnife.apply(mTitles,
        (view, index) -> view.setTextColor(accentColor));

    //封面
    mOriginalAlbumChoice = SPUtil
        .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.AUTO_DOWNLOAD_ALBUM_COVER,
            mContext.getString(R.string.always));
    mAlbumCoverText.setText(mOriginalAlbumChoice);

    //根据系统版本决定是否显示通知栏样式切换
    findViewById(R.id.setting_classic_notify_container)
        .setVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? View.VISIBLE : View.GONE);

    //当前主题
    mThemeText.setText(getThemeText());

    //锁屏样式
    int lockScreen = SPUtil
        .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCKSCREEN,
            Constants.CASSETTE_LOCKSCREEN);
    mLockScreenTip.setText(lockScreen == 0 ? R.string.cassette_lockscreen_tip :
        lockScreen == 1 ? R.string.system_lockscreen_tip : R.string.lockscreen_off_tip);

    //计算缓存大小
    new Thread() {
      @Override
      public void run() {
        mCacheSize = 0;
        mCacheSize += Util.getFolderSize(getExternalCacheDir());
        mCacheSize += Util.getFolderSize(getCacheDir());
        mHandler.sendEmptyMessage(CACHE_SIZE);
      }
    }.start();

    if (IS_GOOGLEPLAY) {
      findViewById(R.id.setting_update_container).setVisibility(View.GONE);
    }
  }


  @Override
  public void onBackPressed() {
    Intent intent = getIntent();
    intent.putExtra(EXTRA_RECREATE, mNeedRecreate);
    intent.putExtra(EXTRA_REFRESH_ADAPTER, mNeedRefreshAdapter);
    intent.putExtra(EXTRA_REFRESH_LIBRARY, mNeedRefreshLibrary);
    setResult(Activity.RESULT_OK, intent);
    finish();
  }

  @Override
  protected void onClickNavigation() {
    onBackPressed();
  }

  @Override
  public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
    String tag = dialog.getTag();
    String playListName = "";
    try {
      if (tag.contains("ExportPlayList")) {
        String[] tagAndName = tag.split("-");
        tag = tagAndName[0];
        playListName = tagAndName[1];
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    switch (tag) {
//            case "Lrc":
//                boolean success = SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCAL_LYRIC_SEARCH_DIR, folder.getAbsolutePath());
//                ToastUtil.show(this, success ? R.string.setting_success : R.string.setting_error, Toast.LENGTH_SHORT);
//                mLrcPath.setText(getString(R.string.lrc_tip, SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCAL_LYRIC_SEARCH_DIR, "")));
//                break;
      case "Scan":
        new MediaScanner(mContext).scanFiles(folder);
        mNeedRefreshAdapter = true;
        break;
      case "ExportPlayList":
        if (TextUtils.isEmpty(playListName)) {
          ToastUtil.show(mContext, R.string.export_fail);
          return;
        }
        mDisposables
            .add(exportPlayListToFile(this, playListName, new File(folder, playListName.concat(".m3u"))));
        break;
    }
  }

  @Override
  public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {
    switch (dialog.getTag()) {
      case "Import":
        final String newPlaylistName = file.getName().substring(0, file.getName().lastIndexOf("."));

        DatabaseRepository.getInstance()
            .getAllPlaylist()
            .map(new Function<List<PlayList>, List<String>>() {
              @Override
              public List<String> apply(List<PlayList> playLists) throws Exception {
                List<String> allPlayListsName = new ArrayList<>();
                //判断是否存在
                boolean alreadyExist = false;
                for (PlayList playList : playLists) {
                  allPlayListsName.add(playList.getName());
                  if (playList.getName().equalsIgnoreCase(newPlaylistName)) {
                    alreadyExist = true;
                  }
                }
                //不存在则提示新建
                if (!alreadyExist) {
                  allPlayListsName.add(0, newPlaylistName + "(" + getString(R.string.new_create) + ")");
                }

                return allPlayListsName;
              }
            })
            .compose(applySingleScheduler())
            .subscribe(new Consumer<List<String>>() {
              @Override
              public void accept(List<String> allPlayListsName) throws Exception {
                Theme.getBaseDialog(mContext)
                    .title(R.string.import_playlist_to)
                    .items(allPlayListsName)
                    .itemsCallback((dialog1, itemView, position, text) -> {
                      final boolean chooseNew =
                          position == 0 && text.toString().endsWith(
                              "(" + getString(R.string.new_create) + ")");
                      mDisposables.add(
                          importM3UFile(SettingActivity.this,file, chooseNew ? newPlaylistName : text.toString(), chooseNew));
                    })
                    .show();
              }
            });

        break;
    }

  }

  @Override
  public void onFileChooserDismissed(@NonNull FileChooserDialog dialog) {

  }


  @Override
  public void onColorSelection(@NonNull ColorChooserDialog dialog, int selectedColor) {
    switch (dialog.getTitle()) {
      case R.string.primary_color:
        saveMaterialPrimaryColor(selectedColor);
        break;
      case R.string.accent_color:
        saveAccentColor(selectedColor);
        break;
    }
    mNeedRecreate = true;
    recreate();
  }

  @SuppressLint("CheckResult")
  @OnClick({R.id.setting_filter_container, R.id.setting_primary_color_container,
      R.id.setting_notify_color_container,
      R.id.setting_feedback_container, R.id.setting_update_container,
      R.id.setting_lockscreen_container, R.id.setting_lrc_priority_container,
      R.id.setting_lrc_float_container,
      R.id.setting_navigation_container, R.id.setting_shake_container, R.id.setting_eq_container,
      R.id.setting_clear_container, R.id.setting_breakpoint_container,
      R.id.setting_screen_container,
      R.id.setting_scan_container, R.id.setting_classic_notify_container,
      R.id.setting_album_cover_container,
      R.id.setting_library_category_container, R.id.setting_immersive_container,
      R.id.setting_import_playlist_container,
      R.id.setting_export_playlist_container, R.id.setting_ignore_mediastore_container,
      R.id.setting_cover_source_container,
      R.id.setting_player_bottom_container, R.id.setting_restore_delete_container,
      R.id.setting_displayname_container,
      R.id.setting_general_theme_container, R.id.setting_accent_color_container,
      R.id.setting_language_container})
  public void onClick(View v) {
    switch (v.getId()) {
      //文件过滤
      case R.id.setting_filter_container:
        configFilterSize();
        break;
      //曲库
      case R.id.setting_library_category_container:
        configLibraryCategory();
        break;
      //桌面歌词
      case R.id.setting_lrc_float_container:
//                if((!mFloatLrcSwitch.isChecked() && FloatWindowManager.getInstance().checkPermission(this)) || mFloatLrcSwitch.isChecked()){
//                    mFloatLrcSwitch.setChecked(!mFloatLrcSwitch.isChecked());
//                }
        mFloatLrcSwitch.setChecked(!mFloatLrcSwitch.isChecked());
        break;
//            //歌词扫描路径
//            case R.id.setting_lrc_path_container:
//                new FolderChooserDialog.Builder(this)
//                        .chooseButton(R.string.choose_folder)
//                        .allowNewFolder(false, R.string.new_folder)
//                        .tag("Lrc")
//                        .show();
//                break;
      //歌词搜索优先级
      case R.id.setting_lrc_priority_container:
        configLyricPriority();
        break;
      //屏幕常亮
      case R.id.setting_screen_container:
        mScreenSwitch.setChecked(!mScreenSwitch.isChecked());
        break;
      //手动扫描
      case R.id.setting_scan_container:
        new FolderChooserDialog.Builder(this)
            .chooseButton(R.string.choose_folder)
            .tag("Scan")
            .allowNewFolder(false, R.string.new_folder)
            .show();
        break;
      //锁屏显示
      case R.id.setting_lockscreen_container:
        configLockScreen();

        break;
      //导航栏变色
      case R.id.setting_navigation_container:
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
          ToastUtil.show(this, getString(R.string.only_lollopop));
          return;
        }
        mNaviSwitch.setChecked(!mNaviSwitch.isChecked());
        break;
      //摇一摇
      case R.id.setting_shake_container:
        mShakeSwitch.setChecked(!mShakeSwitch.isChecked());
        break;
      //选择主色调
      case R.id.setting_primary_color_container:
        new ColorChooserDialog.Builder(SettingActivity.this, R.string.primary_color)
            .accentMode(false)
            .preselect(ThemeStore.getMaterialPrimaryColor())
            .allowUserColorInput(true)
            .allowUserColorInputAlpha(false)
            .show();
        break;
      //选择强调色
      case R.id.setting_accent_color_container:
        new ColorChooserDialog.Builder(SettingActivity.this, R.string.accent_color)
            .accentMode(true)
            .preselect(ThemeStore.getAccentColor())
            .allowUserColorInput(true)
            .allowUserColorInputAlpha(false)
            .show();
        break;
      //通知栏底色
      case R.id.setting_notify_color_container:
        configNotifyBackgroundColor();
        break;
      //音效设置
      case R.id.setting_eq_container:
        startEqualizer();
        break;
      //意见与反馈
      case R.id.setting_feedback_container:
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/Pgdbsg9")));
        Toast.makeText(mContext, getString(R.string.opening_tanzent_lab_discord), Toast.LENGTH_SHORT).show();
        break;
      //检查更新
      case R.id.setting_update_container:
        final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
        try {
          startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
          startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
        break;
      //清除缓存
      case R.id.setting_clear_container:
        clearCache();
        break;
      //通知栏样式
      case R.id.setting_classic_notify_container:
        mNotifyStyleSwitch.setChecked(!mNotifyStyleSwitch.isChecked());
        break;
      //专辑与艺术家封面自动下载
      case R.id.setting_album_cover_container:
        configCoverDownload();
        break;
      //封面下载源
      case R.id.setting_cover_source_container:
        configCoverDownloadSource();
        break;
      //沉浸式状态栏
      case R.id.setting_immersive_container:
        mImmersiveSwitch.setChecked(!mImmersiveSwitch.isChecked());
        break;
      //歌单导入
      case R.id.setting_import_playlist_container:
        importPlayList();
        break;
      //歌单导出
      case R.id.setting_export_playlist_container:
        exportPlayList();
        break;
      //断点播放
      case R.id.setting_breakpoint_container:
        mBreakpointSwitch.setChecked(!mBreakpointSwitch.isChecked());
        break;
      //忽略内嵌封面
      case R.id.setting_ignore_mediastore_container:
        mIgnoreMediastoreSwitch.setChecked(!mIgnoreMediastoreSwitch.isChecked());
        break;
      //播放界面底部
      case R.id.setting_player_bottom_container:
        changeBottomOfPlayingScreen();
        break;
      //恢复移除的歌曲
      case R.id.setting_restore_delete_container:
        restoreDeleteSong();
        break;
      //文件名
      case R.id.setting_displayname_container:
        mShowDisplaynameSwitch.setChecked(!mShowDisplaynameSwitch.isChecked());
        break;
      //全局主题
      case R.id.setting_general_theme_container:
        configGeneralTheme();
        break;
      //语言
      case R.id.setting_language_container:
        changeLanguage();
        break;
    }
  }

  private void changeLanguage() {
    final String ko = getString(R.string.ko);
    final String english = getString(R.string.english);
    final String auto = getString(R.string.auto);

    getBaseDialog(this)
        .items(new String[]{auto, ko, english})
        .itemsCallbackSingleChoice(SPUtil.getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.LANGUAGE, AUTO),
            (dialog, itemView, which, text) -> {
              LanguageHelper.saveSelectLanguage(mContext,which);

              Intent intent = new Intent(mContext, MainActivity.class);
              intent.setAction(Intent.ACTION_MAIN);
              intent.addCategory(Intent.CATEGORY_LAUNCHER);
              intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
              startActivity(intent);
              return true;
            })
        .show();
  }


  /**
   * 配置全局主题
   */
  private void configGeneralTheme() {
    getBaseDialog(this)
        .items(R.array.general_theme)
        .itemsCallback((dialog, itemView, position, text) -> {
          String valTheme = getThemeText();
          if (!text.equals(valTheme)) {
            ThemeStore.setGeneralTheme(position);
            mThemeText.setText(text);
            mNeedRecreate = true;
            recreate();
          }
        }).show();
  }


  /**
   * 恢复移除的歌曲
   */
  private void restoreDeleteSong() {
    SPUtil.deleteValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST_SONG);
    getContentResolver().notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null);
    ToastUtil.show(mContext, R.string.alread_restore_songs);
    Intent intent = new Intent(mContext, MainActivity.class);
    intent.setAction(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  /**
   * 播放列表导出
   */
  private void exportPlayList() {
    mDisposables.add(DatabaseRepository.getInstance()
        .getAllPlaylist()
        .map(playLists -> {
          List<String> allplayListNames = new ArrayList<>();
          for (PlayList playList : playLists) {
            allplayListNames.add(playList.getName());
          }
          return allplayListNames;
        })
        .compose(applySingleScheduler())
        .subscribe(allPlayListNames -> getBaseDialog(mContext)
            .title(R.string.choose_playlist_to_export)
            .negativeText(R.string.cancel)
            .items(allPlayListNames)
            .itemsCallback((dialog, itemView, pos, text) ->
                new FolderChooserDialog.Builder(SettingActivity.this)
                    .chooseButton(R.string.choose_folder)
                    .tag("ExportPlayList-" + text)
                    .allowNewFolder(true, R.string.new_folder)
                    .show())
            .show()));
  }

  /**
   * 播放列表导入
   */
  @SuppressLint("CheckResult")
  private void importPlayList() {
    getBaseDialog(mContext)
        .title(R.string.choose_import_way)
        .negativeText(R.string.cancel)
        .items(new String[]{getString(R.string.import_from_external_storage),
            getString(R.string.import_from_others)})
        .itemsCallback((dialog, itemView, select, text) -> {
          if (select == 0) {
            new FileChooserDialog.Builder(SettingActivity.this)
                .tag("Import")
                .extensionsFilter(".m3u")
                .show();
          } else {
            Single
                .fromCallable(() -> DatabaseRepository.getInstance().getPlaylistFromMediaStore())
                .compose(applySingleScheduler())
                .subscribe(localPlayLists -> {
                  if (localPlayLists == null || localPlayLists.size() == 0) {
                    ToastUtil.show(mContext, R.string.import_fail,
                        getString(R.string.no_playlist_can_import));
                    return;
                  }
                  List<Integer> selectedIndices = new ArrayList<>();
                  for (int i = 0; i < localPlayLists.size(); i++) {
                    selectedIndices.add(i);
                  }
                  getBaseDialog(mContext)
                      .title(R.string.choose_import_playlist)
                      .positiveText(R.string.choose)
                      .items(localPlayLists.keySet())
                      .itemsCallbackMultiChoice(
                          selectedIndices.toArray(new Integer[0]),
                          (dialog1, which, allSelects) -> {
                            mDisposables.add(importLocalPlayList(localPlayLists, allSelects));
                            return true;
                          }).show();

                }, throwable -> ToastUtil.show(mContext, R.string.import_fail, throwable.toString()));
          }
        })
        .theme(ThemeStore.getMDDialogTheme())
        .show();
  }

  /**
   * 歌词搜索优先级
   */
  private void configLyricPriority() {
    LyricPriorityDialog.newInstance().show(getSupportFragmentManager(), "configLyricPriority");
  }

  /**
   * 配置封面下载源
   */
  private void configCoverDownloadSource() {
    final int oldChoice = SPUtil
        .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_COVER_DOWNLOAD_SOURCE,
            DOWNLOAD_LASTFM);
    getBaseDialog(mContext)
        .title(R.string.cover_download_source)
        .items(new String[]{getString(R.string.lastfm), getString(R.string.netease)})
        .itemsCallbackSingleChoice(oldChoice,
            (dialog, view, which, text) -> {
              if (oldChoice != which) {
                mNeedRefreshAdapter = true;
                ImageUriRequest.DOWNLOAD_SOURCE = which;
                SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME,
                    SPUtil.SETTING_KEY.ALBUM_COVER_DOWNLOAD_SOURCE, which);
              }
              return true;
            })
        .show();
  }

  /**
   * 配置封面是否下载
   */
  private void configCoverDownload() {
    final String choice = SPUtil
        .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.AUTO_DOWNLOAD_ALBUM_COVER,
            mContext.getString(R.string.always));
    getBaseDialog(mContext)
        .title(R.string.auto_download_album_artist_cover)
        .items(new String[]{getString(R.string.always), getString(R.string.wifi_only),
            getString(R.string.never)})
        .itemsCallbackSingleChoice(mContext.getString(R.string.wifi_only).equals(choice) ? 1
                : mContext.getString(R.string.always).equals(choice) ? 0 : 2,
            (dialog, view, which, text) -> {
              mAlbumCoverText.setText(text);
              //仅从从不改变到仅在wifi下或者总是的情况下，才刷新Adapter
              mNeedRefreshAdapter |= (
                  (mContext.getString(R.string.wifi_only).contentEquals(text) | mContext
                      .getString(R.string.always).contentEquals(text))
                      & !mOriginalAlbumChoice.contentEquals(text));
              clearDownloadCover(text);
              ImageUriRequest.AUTO_DOWNLOAD_ALBUM = text.toString();
              SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME,
                  SPUtil.SETTING_KEY.AUTO_DOWNLOAD_ALBUM_COVER,
                  text.toString());
              return true;
            }).show();
  }

  /**
   * 当用户选择从不下载时询问是否清除已有封面
   */
  private void clearDownloadCover(CharSequence text) {
    if (getString(R.string.never).contentEquals(text)) {
      getBaseDialog(mContext)
          .title(R.string.clear_download_cover)
          .positiveText(R.string.confirm)
          .negativeText(R.string.cancel)
          .onPositive((clearDialog, action) -> {
            SPUtil.deleteFile(mContext, SPUtil.COVER_KEY.NAME);
            Fresco.getImagePipeline().clearCaches();
            mNeedRefreshAdapter = true;
          }).show();
    }
  }

  /**
   * 清除缓存
   */
  private void clearCache() {
    getBaseDialog(mContext)
        .content(R.string.confirm_clear_cache)
        .positiveText(R.string.confirm)
        .negativeText(R.string.cancel)
        .onPositive((dialog, which) -> new Thread() {
          @Override
          public void run() {
            //清除歌词，封面等缓存
            //清除配置文件、数据库等缓存
            Util.deleteFilesByDirectory(getCacheDir());
            Util.deleteFilesByDirectory(getExternalCacheDir());
//                        SPUtil.deleteFile(mContext,SPUtil.SETTING_KEY.NAME);
//                        deleteDatabase(DBOpenHelper.DBNAME);
            //清除fresco缓存
            Fresco.getImagePipeline().clearCaches();
            mHandler.sendEmptyMessage(CLEAR_FINISH);
            mNeedRefreshAdapter = true;
          }
        }.start()).show();
  }

  /**
   * 启动均衡器
   */
  private void startEqualizer() {
    final int sessionId = MusicServiceRemote.getMediaPlayer().getAudioSessionId();
    if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
      Toast.makeText(mContext, getResources().getString(R.string.no_audio_ID), Toast.LENGTH_LONG)
          .show();
      return;
    }
    Intent audioEffectIntent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
    audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
        MusicServiceRemote.getMediaPlayer().getAudioSessionId());
    audioEffectIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
    if (Util.isIntentAvailable(this, audioEffectIntent)) {
      startActivityForResult(audioEffectIntent, REQUEST_EQ);
    } else {
      ToastUtil.show(mContext, R.string.no_equalizer);
//            startActivity(new Intent(this, EQActivity.class));
    }
  }

  /**
   * 配置通知栏底色
   */
  private void configNotifyBackgroundColor() {
    if (!SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.NOTIFY_STYLE_CLASSIC,
        false)) {
      ToastUtil.show(mContext, R.string.notify_bg_color_warnning);
      return;
    }
    getBaseDialog(mContext)
        .title(R.string.notify_bg_color)
        .items(
            new String[]{getString(R.string.use_system_color), getString(R.string.use_black_color)})
        .itemsCallbackSingleChoice(SPUtil
                .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.NOTIFY_SYSTEM_COLOR,
                    true) ? 0 : 1,
            (dialog, view, which, text) -> {
              SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME,
                  SPUtil.SETTING_KEY.NOTIFY_SYSTEM_COLOR, which == 0);
              sendLocalBroadcast(new Intent(MusicService.ACTION_CMD)
                  .putExtra("Control", Command.TOGGLE_NOTIFY)
                  .putExtra(SPUtil.SETTING_KEY.NOTIFY_STYLE_CLASSIC,
                      mNotifyStyleSwitch.isChecked()));
              return true;
            })
        .show();
  }

  /**
   * 配置锁屏界面
   */
  private void configLockScreen() {
    //0:APlayer锁屏 1:系统锁屏 2:关闭
    getBaseDialog(mContext)
        .title(R.string.lockscreen_show)
        .items(new String[]{getString(R.string.cassette_lockscreen),
            getString(R.string.system_lockscreen), getString(R.string.close)})
        .itemsCallbackSingleChoice(SPUtil
                .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCKSCREEN,
                    Constants.CASSETTE_LOCKSCREEN),
            (dialog, view, which, text) -> {
              SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCKSCREEN,
                  which);
              mLockScreenTip.setText(which == 0 ? R.string.cassette_lockscreen_tip :
                  which == 1 ? R.string.system_lockscreen_tip : R.string.lockscreen_off_tip);
              Intent intent = new Intent(MusicService.ACTION_CMD);
              intent.putExtra("Control", Command.TOGGLE_MEDIASESSION);
              sendLocalBroadcast(intent);
              return true;
            }).show();
  }

  /**
   * 配置曲库目录
   */
  private void configLibraryCategory() {
    String categoryJson = SPUtil
        .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY, "");

    List<Category> oldCategories = new Gson()
        .fromJson(categoryJson, new TypeToken<List<Category>>() {
        }.getType());
    if (oldCategories == null || oldCategories.size() == 0) {
      ToastUtil.show(mContext, getString(R.string.load_failed));
      return;
    }
    List<Integer> selected = new ArrayList<>();
    for (Category temp : oldCategories) {
      selected.add(temp.getOrder());
    }

    final List<String> allLibraryStrings = Category.getAllLibraryString(this);
    getBaseDialog(mContext)
        .title(R.string.library_category)
        .positiveText(R.string.confirm)
        .items(allLibraryStrings)
        .itemsCallbackMultiChoice(selected.toArray(new Integer[selected.size()]),
            (dialog, which, text) -> {
              if (text.length == 0) {
                ToastUtil.show(mContext, getString(R.string.plz_choose_at_least_one_category));
                return true;
              }
              ArrayList<Category> newCategories = new ArrayList<>();
              for (Integer choose : which) {
                newCategories.add(new Category(choose));
              }
              if (!newCategories.equals(oldCategories)) {
                mNeedRefreshLibrary = true;
                getIntent().putExtra(EXTRA_CATEGORY, newCategories);
                SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME,
                    SPUtil.SETTING_KEY.LIBRARY_CATEGORY,
                    new Gson().toJson(newCategories, new TypeToken<List<Category>>() {
                    }.getType()));
              }
              return true;
            }).show();
  }

  /**
   * 配置过滤大小
   */
  private void configFilterSize() {
    //读取以前设置
    int position = 0;
    for (int i = 0; i < mScanSize.length; i++) {
      position = i;
      if (mScanSize[i] == MediaStoreUtil.SCAN_SIZE) {
        break;
      }
    }
    getBaseDialog(mContext)
        .title(R.string.set_filter_size)
        .items(new String[]{"0K", "500K", "1MB", "2MB"})
        .itemsCallbackSingleChoice(position, (dialog, itemView, which, text) -> {
          SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SCAN_SIZE,
              mScanSize[which]);
          MediaStoreUtil.SCAN_SIZE = mScanSize[which];
          getContentResolver().notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null);
          return true;
        }).show();
  }

  private void changeBottomOfPlayingScreen() {
    final int position = SPUtil
        .getValue(mContext, SPUtil.SETTING_KEY.NAME, BOTTOM_OF_NOW_PLAYING_SCREEN, PlayerActivity.BOTTOM_SHOW_BOTH);
    getBaseDialog(mContext)
        .title(R.string.show_on_bottom)
        .items(new String[]{getString(R.string.show_next_song_only),
            getString(R.string.show_vol_control_only),
            getString(R.string.tap_to_toggle)
            , getString(R.string.close)})
        .itemsCallbackSingleChoice(position, (dialog, itemView, which, text) -> {
          if (position != which) {
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, BOTTOM_OF_NOW_PLAYING_SCREEN, which);
          }
          return true;
        }).show();
  }

  @OnHandleMessage
  public void handleInternal(Message msg) {
    if (msg.what == RECREATE) {
      recreate();
    }
    if (msg.what == CACHE_SIZE) {
      mCache.setText(getString(R.string.cache_size, mCacheSize / 1024f / 1024));
    }
    if (msg.what == CLEAR_FINISH) {
      ToastUtil.show(mContext, getString(R.string.clear_success));
      mCache.setText(R.string.zero_size);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(EXTRA_RECREATE, mNeedRecreate);
//        outState.putBoolean("fromColorChoose", mFromColorChoose);
    outState.putBoolean(EXTRA_REFRESH_ADAPTER, mNeedRefreshAdapter);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mHandler.remove();
    if (mDisposables != null) {
      for (Disposable disposable : mDisposables) {
        if (!disposable.isDisposed()) {
          disposable.dispose();
        }
      }
    }

  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_THEME_COLOR) {
      if (data != null) {
//                mFromColorChoose = data.getBooleanExtra("fromColorChoose", false);
        if (mNeedRecreate = data.getBooleanExtra(EXTRA_RECREATE, false)) {
          mHandler.sendEmptyMessage(RECREATE);
        }
      }
    } else if (requestCode == REQUEST_EQ) {

    }
  }

}
