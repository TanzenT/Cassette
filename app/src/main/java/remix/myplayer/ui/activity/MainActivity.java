package remix.myplayer.ui.activity;


import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.PagerAdapter;
import remix.myplayer.fragment.AlbumFragment;
import remix.myplayer.fragment.ArtistFragment;
import remix.myplayer.fragment.BottomActionBarFragment;
import remix.myplayer.fragment.FolderFragment;
import remix.myplayer.fragment.SongFragment;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.dialog.TimerDialog;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DBUtil;
import remix.myplayer.util.DiskCache;
import remix.myplayer.util.Global;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.XmlUtil;

/**
 *
 */
public class MainActivity extends ToolbarActivity implements MusicService.Callback {
    public static MainActivity mInstance = null;
    @BindView(R.id.multi_menu)
    RelativeLayout mMultiMenu;
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
    @BindView(R.id.tabs)
    TabLayout mTablayout;
    @BindView(R.id.ViewPager)
    android.support.v4.view.ViewPager mViewPager;
    @BindView(R.id.navigation_view)
    NavigationView mNavigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    private BottomActionBarFragment mBottomBar;
    private final static String TAG = "MainActivity";

    private PagerAdapter mAdapter;
    //是否正在运行
    private static boolean mIsRunning = false;
    //是否第一次启动
    private static boolean mIsFirst = true;

    public static MultiChoice MultiChoice = new MultiChoice();

    private static final int PERMISSIONCODE = 100;
    private static final String[] PERMISSIONS = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE};

    //更新主题
    private final int UPDATE_THEME = 1;
    private Handler mRefreshHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == Constants.RECREATE_ACTIVITY) {
                recreate();
            }
            else if(msg.what == Constants.UPDATE_MULTI){
                MultiChoice.clearSelectedViews();
            }
            else if(msg.what == Constants.UPDATE_ADAPTER){
                //刷新适配器
                for(Fragment temp : getSupportFragmentManager().getFragments()){
                    if(temp instanceof SongFragment){
                       SongFragment songFragment = (SongFragment)temp;
                        if(songFragment.getAdapter() != null && songFragment.getUserVisibleHint())
                            songFragment.getAdapter().notifyDataSetChanged();
                    }
                    if(temp instanceof AlbumFragment){
                        AlbumFragment albumFragment = (AlbumFragment)temp;
                        if(albumFragment.getAdapter() != null && albumFragment.getUserVisibleHint())
                            albumFragment.getAdapter().notifyDataSetChanged();
                    }
                    if(temp instanceof ArtistFragment){
                        ArtistFragment artistFragment = (ArtistFragment)temp;
                        if(artistFragment.getAdapter() != null && artistFragment.getUserVisibleHint())
                            artistFragment.getAdapter().notifyDataSetChanged();
                    }
                    if(temp instanceof FolderFragment){
                        FolderFragment folderFragment = (FolderFragment) temp;
                        if(folderFragment.getAdapter() != null && folderFragment.getUserVisibleHint()){
                            folderFragment.getAdapter().notifyDataSetChanged();
                        }
                    }
                }
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        if(MultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
        }
        mIsRunning = true;
        //更新UI
        UpdateUI(MusicService.getCurrentMP3(), MusicService.getIsplay());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(MultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessageDelayed(Constants.UPDATE_MULTI,500);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        mInstance = this;

        MultiChoice.setOnUpdateOptionMenuListener(new MultiChoice.onUpdateOptionMenuListener() {
            @Override
            public void onUpdate(boolean multiShow) {
                MultiChoice.setShowing(multiShow);
                mToolBar.setNavigationIcon(MultiChoice.isShow() ? R.drawable.actionbar_delete : R.drawable.actionbar_menu);
                mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(MultiChoice.isShow()){
                            MultiChoice.UpdateOptionMenu(false);
                            MultiChoice.clear();
                        } else {
                            mDrawerLayout.openDrawer(mNavigationView);
                        }
                    }
                });
                if(!MultiChoice.isShow()){
                    MultiChoice.clear();
                }
                invalidateOptionsMenu();
            }
        });

        //播放的service
        MusicService.addCallback(MainActivity.this);

        //初始化toolbar
        initToolbar(mToolBar,"");
        initPager();
        initTab();
        //初始化测滑菜单
        initDrawerLayout();
        //根据主题设置颜色
        initColor();
        //初始化底部状态栏
        mBottomBar = (BottomActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);

        boolean isFirst = SPUtil.getValue(this, "Setting", "First", true);
        SPUtil.putValue(this, "Setting", "First", false);

        //第一次启动软件
        if(isFirst){
            //保存默认主题设置
            SPUtil.putValue(this,"Setting","ThemeMode",ThemeStore.DAY);
            SPUtil.putValue(this,"Setting","ThemeColor",ThemeStore.THEME_PINK);
            //添加我的收藏列表
            XmlUtil.addPlaylist(this,"我的收藏");
        }
        initLastSong();

    }

    /**
     * 初始化上一次退出时播放的歌曲
     * 默认为第一首歌曲
     */
    private void initLastSong() {
        //读取上次退出时正在播放的歌曲的id
        int lastId = SPUtil.getValue(this,"Setting","LastSongId",0);
        //上次退出时正在播放的歌曲是否还存在
        boolean isLastSongExist = false;
        //上次退出时正在播放的歌曲的pos
        int pos = 0;
        //查找上次退出时的歌曲是否还存在

        for(int i = 0 ; i < Global.mAllSongList.size();i++){
            if(lastId == Global.mAllSongList.get(i)){
                isLastSongExist = true;
                pos = i;
                break;
            }
        }

        boolean isPlay = !mIsFirst && MusicService.getIsplay();
        if(mIsFirst){
            mIsFirst = false;
            MP3Item item = null;
            //上次退出时保存的正在播放的歌曲已失效
            if(isLastSongExist && (item = DBUtil.getMP3InfoById(lastId)) != null) {
                mBottomBar.UpdateBottomStatus(item, isPlay);
                MusicService.initDataSource(item,pos);

            }else {

                if(Global.mPlayingList.size() > 0){
                    //重新找到一个歌曲id
                    int id =  Global.mPlayingList.get(0);
                    for(int i = 0 ; i < Global.mPlayingList.size() ;i++){
                        id = Global.mPlayingList.get(i);
                        if (id != lastId)
                            break;
                    }
                    item = DBUtil.getMP3InfoById(id);
                    mBottomBar.UpdateBottomStatus(item,isPlay);
                    SPUtil.putValue(this,"Setting","LastSongId",id);
                    MusicService.initDataSource(item,0);

                }
            }
        } else {
            mBottomBar.UpdateBottomStatus(MusicService.getCurrentMP3(), MusicService.getIsplay());
        }
    }

    /**
     * 初始化主题
     */
    private void initTheme() {
        ThemeStore.THEME_MODE = ThemeStore.loadThemeMode();
        ThemeStore.THEME_COLOR = ThemeStore.loadThemeColor();

        ThemeStore.MATERIAL_COLOR_PRIMARY = ThemeStore.getMaterialPrimaryColorRes();
        ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = ThemeStore.getMaterialPrimaryDarkColorRes();
        Log.d(TAG,"primary:" + ThemeStore.MATERIAL_COLOR_PRIMARY + "\r\nprimary dark:" + ThemeStore.MATERIAL_COLOR_PRIMARY_DARK);
//        int color = ThemeStore.getMaterialPrimaryColorRes(this);
//        ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = ThemeStore.getMaterialPrimaryColorRes(ThemeStore.THEME_COLOR);
    }

    private void initColor() {
        mToolBar.setBackgroundColor(ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY));
        mTablayout.setBackgroundColor(ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY));
    }


    @Override
    protected void setStatusBar() {
        StatusBarUtil.setColorNoTranslucentForDrawerLayout(this,
                (DrawerLayout) findViewById(R.id.drawer_layout),
                ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY_DARK));
    }

    @Override
    protected void initToolbar(Toolbar toolbar, String title) {
        mToolBar.setTitle("");

        setSupportActionBar(mToolBar);
        mToolBar.setNavigationIcon(R.drawable.actionbar_menu);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(mNavigationView);
            }
        });

        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.toolbar_search:
                        startActivity(new Intent(MainActivity.this, SearchActivity.class));
                        break;
                    case R.id.toolbar_timer:
                        startActivity(new Intent(MainActivity.this, TimerDialog.class));
                        break;
                    case R.id.toolbar_delete:
                        Test(MultiChoice);
                        break;
                    case R.id.toolbar_add_playing:
                        Test(MultiChoice);
                        break;
                    case R.id.toolbar_add_playlist:
                        Test(MultiChoice);
                        break;
                }
                return true;
            }
        });

    }


    public PagerAdapter getAdapter() {
        return mAdapter;
    }

    public ViewPager getViewPager() {
        return mViewPager;
    }

    //初始化ViewPager
    private void initPager() {
        mAdapter = new PagerAdapter(getSupportFragmentManager());
        mAdapter.setTitles(new String[]{getResources().getString(R.string.tab_song),
                getResources().getString(R.string.tab_album),
                getResources().getString(R.string.tab_artist),
                getResources().getString(R.string.tab_folder)});
        mAdapter.AddFragment(new SongFragment());
        mAdapter.AddFragment(new AlbumFragment());
        mAdapter.AddFragment(new ArtistFragment());
        mAdapter.AddFragment(new FolderFragment());

        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(0);
    }

    //初始化custontab
    private void initTab() {
        //添加tab选项卡
        mTablayout.addTab(mTablayout.newTab().setText(getResources().getString(R.string.tab_song)));
        mTablayout.addTab(mTablayout.newTab().setText(getResources().getString(R.string.tab_album)));
        mTablayout.addTab(mTablayout.newTab().setText(getResources().getString(R.string.tab_artist)));
        mTablayout.addTab(mTablayout.newTab().setText(getResources().getString(R.string.tab_folder)));
        //viewpager与tablayout关联
        mTablayout.setupWithViewPager(mViewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(MultiChoice.isShow() ? R.menu.multi_menu : R.menu.toolbar_menu, menu);
        return true;
    }

    private void initDrawerLayout() {
        mNavigationView.setItemTextAppearance(R.style.Drawer_text_style);
        ColorStateList colorStateList = new ColorStateList(new int[][]{{android.R.attr.state_pressed},{android.R.attr.state_checked} ,{}},
                new int[]{ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY), ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY),ColorUtil.getColor(R.color.black_737373)});
        mNavigationView.setItemIconTintList(colorStateList);
        mNavigationView.setItemTextColor(colorStateList);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                item.setChecked(true);
                switch (item.getItemId()) {
                    case R.id.item_recently:
                        //最近添加
                        startActivity(new Intent(MainActivity.this, RecetenlyActivity.class));
                        break;
                    case R.id.item_playlist:
                        startActivity(new Intent(MainActivity.this, PlayListActivity.class));
                        break;
                    case R.id.item_allsong:
                        mDrawerLayout.closeDrawer(mNavigationView);
                        break;
                    case R.id.item_setting:
                        //设置
                        startActivityForResult(new Intent(MainActivity.this,SettingActivity.class),UPDATE_THEME);
//                        startActivityForResult(new Intent(MainActivity.this,ThemeActivity.class),UPDATE_THEME);
                        break;
                    case R.id.item_exit:
                        sendBroadcast(new Intent(Constants.EXIT));
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data != null){
            boolean isAlbum = Global.mAlbunOrArtist == Constants.ALBUM;
            String errorTxt = isAlbum ? "设置专辑封面失败" : "设置艺术家封面失败";
            int id = Global.mAlbumArtistID; //专辑或艺术家封面
            String name = Global.mAlbumArtistName;
            switch (requestCode){
                //重启activity
                case UPDATE_THEME:
                    if(data.getBooleanExtra("needRefresh",false))
                        mRefreshHandler.sendEmptyMessage(Constants.RECREATE_ACTIVITY);
                    break;
                //图片选择
                case Crop.REQUEST_PICK:
                    if(resultCode == RESULT_OK){
                        File cacheDir = DiskCache.getDiskCacheDir(this,"thumbnail/" + (isAlbum ? "album" : "artist"));
                        if(!cacheDir.exists()){
                            if(!cacheDir.mkdir()){
                                Toast.makeText(this,errorTxt,Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        Uri destination = Uri.fromFile(new File(cacheDir, CommonUtil.hashKeyForDisk((id * 255 ) + "")));
                        Crop.of(data.getData(), destination).asSquare().start(this);
                    } else {
                        Toast.makeText(this,errorTxt,Toast.LENGTH_SHORT).show();
                    }
                    break;
                //图片裁剪
                case Crop.REQUEST_CROP:
                    //裁剪后的图片路径
                    String path = Crop.getOutput(data).getEncodedPath();
                    if(TextUtils.isEmpty(path) || id == -1){
                        Toast.makeText(MainActivity.this, errorTxt, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //清除fresco的缓存
                    ImagePipeline imagePipeline = Fresco.getImagePipeline();
                    Uri fileUri = Uri.parse("file:///" + path);
                    Uri providerUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id);
                    imagePipeline.evictFromCache(fileUri);
                    imagePipeline.evictFromCache(providerUri);
                    mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
                    break;
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawer(mNavigationView);
        } else if(MultiChoice.isShow()) {
//            updateOptionsMenu(false);
            MultiChoice.UpdateOptionMenu(false);
        } else {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
            Intent intent = new Intent(Constants.NOTIFY);
            intent.putExtra("FromMainActivity", true);
            sendBroadcast(intent);
        }
    }

    //更新界面
    @Override
    public void UpdateUI(MP3Item mP3Item, boolean isplay) {
        if (!mIsRunning)
            return;
        mBottomBar.UpdateBottomStatus(mP3Item, isplay);
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragmentList) {
            if (fragment instanceof SongFragment && ((SongFragment) fragment).getAdapter() != null) {
                ((SongFragment) fragment).getAdapter().notifyDataSetChanged();
            }
        }
    }

    @Override
    public int getType() {
        return Constants.MAINACTIVITY;
    }

}

