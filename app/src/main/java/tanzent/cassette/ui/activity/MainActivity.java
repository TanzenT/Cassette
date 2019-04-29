package tanzent.cassette.ui.activity;


import static tanzent.cassette.App.IS_GOOGLEPLAY;
import static tanzent.cassette.request.network.RxUtil.applySingleScheduler;
import static tanzent.cassette.theme.ThemeStore.getMaterialPrimaryColor;
import static tanzent.cassette.theme.ThemeStore.getMaterialPrimaryColorReverse;
import static tanzent.cassette.util.ImageUriUtil.getSearchRequestWithAlbumType;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import tanzent.cassette.App;
import tanzent.cassette.R;
import tanzent.cassette.bean.misc.Category;
import tanzent.cassette.db.room.DatabaseRepository;
import tanzent.cassette.helper.MusicServiceRemote;
import tanzent.cassette.helper.SortOrder;
import tanzent.cassette.misc.handler.MsgHandler;
import tanzent.cassette.misc.handler.OnHandleMessage;
import tanzent.cassette.misc.interfaces.OnItemClickListener;
import tanzent.cassette.misc.receiver.ExitReceiver;
import tanzent.cassette.request.LibraryUriRequest;
import tanzent.cassette.request.RequestConfig;
import tanzent.cassette.service.MusicService;
import tanzent.cassette.theme.Theme;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.ui.adapter.DrawerAdapter;
import tanzent.cassette.ui.adapter.HeaderAdapter;
import tanzent.cassette.ui.adapter.MainPagerAdapter;
import tanzent.cassette.ui.fragment.AlbumFragment;
import tanzent.cassette.ui.fragment.ArtistFragment;
import tanzent.cassette.ui.fragment.FolderFragment;
import tanzent.cassette.ui.fragment.LibraryFragment;
import tanzent.cassette.ui.fragment.PlayListFragment;
import tanzent.cassette.ui.fragment.SongFragment;
import tanzent.cassette.ui.misc.DoubleClickListener;
import tanzent.cassette.ui.misc.MultipleChoice;
import tanzent.cassette.util.ColorUtil;
import tanzent.cassette.util.Constants;
import tanzent.cassette.util.DensityUtil;
import tanzent.cassette.util.MusicUtil;
import tanzent.cassette.util.SPUtil;
import tanzent.cassette.util.StatusBarUtil;
import tanzent.cassette.util.ToastUtil;

/**
 *
 */
public class MainActivity extends MenuActivity {

  public static final String EXTRA_RECREATE = "needRecreate";
  public static final String EXTRA_REFRESH_ADAPTER = "needRefreshAdapter";
  public static final String EXTRA_REFRESH_LIBRARY = "needRefreshLibrary";
  public static final String EXTRA_CATEGORY = "Category";

  public static final long DELAY_HIDE_LOCATION = TimeUnit.SECONDS.toMillis(4);

  @BindView(R.id.tabs)
  TabLayout mTablayout;
  @BindView(R.id.ViewPager)
  ViewPager mViewPager;
  @BindView(R.id.navigation_view)
  NavigationView mNavigationView;
  @BindView(R.id.drawer_layout)
  DrawerLayout mDrawerLayout;
  @BindView(R.id.add)
  ImageView mAddButton;
  @BindView(R.id.header_txt)
  TextView mHeadText;
  @BindView(R.id.header_img)
  SimpleDraweeView mHeadImg;
  @BindView(R.id.header)
  View mHeadRoot;
  @BindView(R.id.recyclerview)
  RecyclerView mRecyclerView;

  private DrawerAdapter mDrawerAdapter;
  private MainPagerAdapter mPagerAdapter;

  private MsgHandler mRefreshHandler;
  //设置界面
  private static final int REQUEST_SETTING = 1;
  //安装权限
  private static final int REQUEST_INSTALL_PACKAGES = 2;

  //当前选中的fragment
  private LibraryFragment mCurrentFragment;

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    //初始化控件
    setUpToolbar();
    setUpPager();
    setUpTab();
    //初始化测滑菜单
    setUpDrawerLayout();
    setUpViewColor();
    //handler
    mRefreshHandler = new MsgHandler(this);
    mRefreshHandler.postDelayed(this::parseIntent, 500);
  }

  @Override
  protected void setStatusBarColor() {
    StatusBarUtil.setColorNoTranslucentForDrawerLayout(this,
        findViewById(R.id.drawer_layout),
        ThemeStore.getStatusBarColor());
  }

  /**
   * 初始化toolbar
   */
  protected void setUpToolbar() {
    super.setUpToolbar("");
    toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
    toolbar.setNavigationOnClickListener(v -> mDrawerLayout.openDrawer(mNavigationView));
  }

  /**
   * 新建播放列表
   */
  @OnClick({R.id.add})
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.add:
        if (MultipleChoice.isActiveSomeWhere()) {
          return;
        }

        DatabaseRepository.getInstance()
            .getAllPlaylist()
            .compose(applySingleScheduler())
            .subscribe(playLists -> Theme.getBaseDialog(mContext)
                .title(R.string.new_playlist)
                .positiveText(R.string.create)
                .negativeText(R.string.cancel)
                .inputRange(1, 15)
                .input("", getString(R.string.local_list) + playLists.size(), (dialog, input) -> {
                  if (!TextUtils.isEmpty(input)) {
                    DatabaseRepository.getInstance()
                        .insertPlayList(input.toString())
                        .compose(applySingleScheduler())
                        .subscribe(id -> {
                          //跳转到添加歌曲界面
                          SongChooseActivity.start(MainActivity.this, id, input.toString());
                        }, throwable -> ToastUtil
                            .show(mContext, R.string.create_playlist_fail, throwable.toString()));
                  }
                })
                .show());
        break;
      default:
        break;
    }
  }

  //初始化ViewPager
  private void setUpPager() {
    String categoryJson = SPUtil
        .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY, "");
    List<Category> categories = TextUtils.isEmpty(categoryJson) ? new ArrayList<>()
        : new Gson().fromJson(categoryJson, new TypeToken<List<Category>>() {
        }.getType());
    if (categories.size() == 0) {
      final List<Category> defaultCategories = Category.getDefaultLibrary(this);
      categories.addAll(defaultCategories);
      SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY,
          new Gson().toJson(defaultCategories, new TypeToken<List<Category>>() {
          }.getType()));
    }
    mPagerAdapter = new MainPagerAdapter(getSupportFragmentManager());
    mPagerAdapter.setList(categories);
    mMenuLayoutId = parseMenuId(mPagerAdapter.getList().get(0).getTag());
    //有且仅有一个tab
    if (categories.size() == 1) {
      if (categories.get(0).isPlayList()) {
        showViewWithAnim(mAddButton, true);
      }
      mTablayout.setVisibility(View.GONE);
    } else {
      mTablayout.setVisibility(View.VISIBLE);
    }

    mViewPager.setAdapter(mPagerAdapter);
    mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount() - 1);
    mViewPager.setCurrentItem(0);
    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
      }

      @Override
      public void onPageSelected(int position) {
        final Category category = mPagerAdapter.getList().get(position);
        showViewWithAnim(mAddButton, category.isPlayList());

        mMenuLayoutId = parseMenuId(mPagerAdapter.getList().get(position).getTag());
        mCurrentFragment = (LibraryFragment) mPagerAdapter.getFragment(position);

        invalidateOptionsMenu();
      }


      @Override
      public void onPageScrollStateChanged(int state) {
      }
    });
    mCurrentFragment = (LibraryFragment) mPagerAdapter.getFragment(0);
  }

  private int mMenuLayoutId = R.menu.menu_main;

  public int parseMenuId(int tag) {
    return tag == Category.TAG_SONG ? R.menu.menu_main :
        tag == Category.TAG_ALBUM ? R.menu.menu_album :
            tag == Category.TAG_ARTIST ? R.menu.menu_artist :
                tag == Category.TAG_PLAYLIST ? R.menu.menu_playlist :
                    tag == Category.TAG_FOLDER ? R.menu.menu_folder : R.menu.menu_main_simple;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    if (mCurrentFragment instanceof FolderFragment) {
      return true;
    }
    String sortOrder = null;
    if (mCurrentFragment instanceof SongFragment) {
      sortOrder = SPUtil
          .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
              SortOrder.SongSortOrder.SONG_A_Z);
    } else if (mCurrentFragment instanceof AlbumFragment) {
      sortOrder = SPUtil
          .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_SORT_ORDER,
              SortOrder.AlbumSortOrder.ALBUM_A_Z);
    } else if (mCurrentFragment instanceof ArtistFragment) {
      sortOrder = SPUtil
          .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER,
              SortOrder.ArtistSortOrder.ARTIST_A_Z);
    } else if (mCurrentFragment instanceof PlayListFragment) {
      sortOrder = SPUtil
          .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER,
              SortOrder.PlayListSortOrder.PLAYLIST_DATE);
    }

    if (TextUtils.isEmpty(sortOrder)) {
      return true;
    }
    setUpMenuItem(menu, sortOrder);
    return true;
  }


  @Override
  public int getMenuLayoutId() {
    return mMenuLayoutId;
  }

  @Override
  protected void saveSortOrder(String sortOrder) {
    if (mCurrentFragment instanceof SongFragment) {
      SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
          sortOrder);
    } else if (mCurrentFragment instanceof AlbumFragment) {
      SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_SORT_ORDER,
          sortOrder);
    } else if (mCurrentFragment instanceof ArtistFragment) {
      SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER,
          sortOrder);
    } else if (mCurrentFragment instanceof PlayListFragment) {
      SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER,
          sortOrder);
    }
    mCurrentFragment.onMediaStoreChanged();
  }

  private void showViewWithAnim(View view, boolean show) {
    if (show) {
      if (view.getVisibility() != View.VISIBLE) {
        view.setVisibility(View.VISIBLE);
        SpringSystem.create().createSpring()
            .addListener(new SimpleSpringListener() {
              @Override
              public void onSpringUpdate(Spring spring) {
                view.setScaleX((float) spring.getCurrentValue());
                view.setScaleY((float) spring.getCurrentValue());
              }
            })
            .setEndValue(1);
      }
    } else {
      view.setVisibility(View.GONE);
    }

  }

  //初始化custontab
  private void setUpTab() {
    //添加tab选项卡
    boolean isPrimaryColorCloseToWhite = ThemeStore.isMDColorCloseToWhite();
//        mTablayout = new TabLayout(new ContextThemeWrapper(this, !ColorUtil.isColorLight(ThemeStore.getMaterialPrimaryColor()) ? R.style.CustomTabLayout_Light : R.style.CustomTabLayout_Dark));
//        mTablayout = new TabLayout(new ContextThemeWrapper(this,R.style.CustomTabLayout_Light));
//        mTablayout.setLayoutParams(new AppBarLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,DensityUtil.dip2px(this,48)));
//        mTablayout = new TabLayout(this);
    mTablayout.setBackgroundColor(getMaterialPrimaryColor());
    mTablayout.addTab(mTablayout.newTab().setText(R.string.tab_song));
    mTablayout.addTab(mTablayout.newTab().setText(R.string.tab_album));
    mTablayout.addTab(mTablayout.newTab().setText(R.string.tab_artist));
    mTablayout.addTab(mTablayout.newTab().setText(R.string.tab_playlist));
    mTablayout.addTab(mTablayout.newTab().setText(R.string.tab_folder));
    //viewpager与tablayout关联
    mTablayout.setupWithViewPager(mViewPager);
    mTablayout.setSelectedTabIndicatorColor(isPrimaryColorCloseToWhite ? Color.BLACK : Color.WHITE);
//        mTablayout.setSelectedTabIndicatorColor(ColorUtil.getColor(isLightColor ? R.color.black : R.color.white));
    mTablayout.setSelectedTabIndicatorHeight(DensityUtil.dip2px(this, 3));
    mTablayout.setTabTextColors(ColorUtil.getColor(
        isPrimaryColorCloseToWhite ? R.color.dark_normal_tab_text_color
            : R.color.light_normal_tab_text_color),
        ColorUtil.getColor(isPrimaryColorCloseToWhite ? R.color.black : R.color.white));

    setTabClickListener();
  }

  private void setTabClickListener() {

    for (int i = 0; i < mTablayout.getTabCount(); i++) {
      TabLayout.Tab tab = mTablayout.getTabAt(i);
      if (tab == null) {
        return;
      }
      Class c = tab.getClass();
      try {
        Field field = c.getDeclaredField("mView");
        field.setAccessible(true);
        final View view = (View) field.get(tab);
        if (view == null) {
          return;
        }
        view.setOnClickListener(new DoubleClickListener() {
          @Override
          public void onDoubleClick(@NotNull View v) {
            // 只有第一个标签可能是"歌曲"
            if (mCurrentFragment instanceof SongFragment) {
              // 滚动到当前的歌曲
              List<Fragment> fragments = getSupportFragmentManager().getFragments();
              for (Fragment fragment : fragments) {
                if (fragment instanceof SongFragment) {
                  ((SongFragment) fragment).scrollToCurrent();
                }
              }
            }
          }
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }


  private void setUpDrawerLayout() {
    mDrawerAdapter = new DrawerAdapter(this, R.layout.item_drawer);
    mDrawerAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        switch (position) {
          //歌曲库
          case 0:
            mDrawerLayout.closeDrawer(mNavigationView);
            break;
          //最近添加
          case 1:
            startActivity(new Intent(mContext, RecentlyActivity.class));
            break;
          //捐赠
          //设置
          case 2:
            startActivityForResult(new Intent(mContext, SettingActivity.class), REQUEST_SETTING);
            break;
          //退出
          case 3:
            sendBroadcast(new Intent(Constants.EXIT)
                .setComponent(new ComponentName(mContext, ExitReceiver.class)));
            break;
        }
        mDrawerAdapter.setSelectIndex(position);
      }

      @Override
      public void onItemLongClick(View view, int position) {
      }
    });
    mRecyclerView.setAdapter(mDrawerAdapter);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

    mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
      @Override
      public void onDrawerSlide(View drawerView, float slideOffset) {
      }

      @Override
      public void onDrawerOpened(View drawerView) {
      }

      @Override
      public void onDrawerClosed(View drawerView) {
        if (mDrawerAdapter != null) {
          mDrawerAdapter.setSelectIndex(0);
        }
      }

      @Override
      public void onDrawerStateChanged(int newState) {
      }
    });
  }

  /**
   * 初始化控件相关颜色
   */
  private void setUpViewColor() {
    //正在播放文字的背景
    GradientDrawable bg = new GradientDrawable();
    final int primaryColor = ThemeStore.getMaterialPrimaryColor();

    bg.setColor(ColorUtil.darkenColor(primaryColor));
    bg.setCornerRadius(DensityUtil.dip2px(this, 4));
    mHeadText.setBackground(bg);
    mHeadText.setTextColor(getMaterialPrimaryColorReverse());
    //抽屉
    mHeadRoot.setBackgroundColor(primaryColor);
    mNavigationView.setBackgroundColor(ThemeStore.getDrawerDefaultColor());

    //这种图片不知道该怎么着色 暂时先这样处理
    mAddButton.setBackground(Theme.tintDrawable(R.drawable.bg_playlist_add,
        ThemeStore.getAccentColor()));
    mAddButton.setImageResource(R.drawable.icon_playlist_add);
  }

  @Override
  public void onMediaStoreChanged() {
    super.onMediaStoreChanged();
    mRefreshHandler.sendEmptyMessage(UPDATE_ADAPTER);
  }

  @SuppressLint("CheckResult")
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case REQUEST_SETTING:
        if (data == null) {
          return;
        }
        if (data.getBooleanExtra(EXTRA_RECREATE, false)) { //设置后需要重启activity
          mRefreshHandler.sendEmptyMessage(RECREATE_ACTIVITY);
        } else if (data.getBooleanExtra(EXTRA_REFRESH_ADAPTER, false)) { //刷新adapter
          clearUriCache();
          mRefreshHandler.sendEmptyMessage(UPDATE_ADAPTER);
        } else if (data.getBooleanExtra(EXTRA_REFRESH_LIBRARY, false)) { //刷新Library
          List<Category> categories = (List<Category>) data.getSerializableExtra(EXTRA_CATEGORY);
          if (categories != null && categories.size() > 0) {
            mPagerAdapter.setList(categories);
            mPagerAdapter.notifyDataSetChanged();
            mViewPager.setOffscreenPageLimit(categories.size() - 1);
            mMenuLayoutId = parseMenuId(
                mPagerAdapter.getList().get(mViewPager.getCurrentItem()).getTag());
            mCurrentFragment = (LibraryFragment) mPagerAdapter
                .getFragment(mViewPager.getCurrentItem());
            invalidateOptionsMenu();
            //如果只有一个Library,隐藏标签栏
            if (categories.size() == 1) {
              mTablayout.setVisibility(View.GONE);
            } else {
              mTablayout.setVisibility(View.VISIBLE);
            }
          }
        }
        break;
    }
  }

  private void clearUriCache() {
    // 清除uriCache
    for (Fragment fragment : getSupportFragmentManager().getFragments()) {
      if (fragment instanceof LibraryFragment) {
        final LibraryFragment libraryFragment = (LibraryFragment) fragment;
        final Adapter adapter = libraryFragment.getAdapter();
        if (adapter instanceof HeaderAdapter) {
          ((HeaderAdapter) adapter).clearUriCache();
        }
      }
    }
  }


  @Override
  public void onBackPressed() {
    if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
      mDrawerLayout.closeDrawer(mNavigationView);
    } else {
      boolean closed = false;
      for (Fragment fragment : getSupportFragmentManager().getFragments()) {
        if (fragment instanceof LibraryFragment) {
          MultipleChoice choice = ((LibraryFragment) fragment).getChoice();
          if (choice.isActive()) {
            closed = true;
            choice.close();
            break;
          }
        }
      }
      if (!closed) {
        super.onBackPressed();
      }
//            Intent intent = new Intent();
//            intent.setAction(Intent.ACTION_MAIN);
//            intent.addCategory(Intent.CATEGORY_HOME);
//            startActivity(intent);
    }
  }

  private static final int IMAGE_SIZE = DensityUtil.dip2px(App.getContext(), 108);

  @Override
  public void onMetaChanged() {
    super.onMetaChanged();
    mHeadText.setText(getString(R.string.play_now, MusicServiceRemote.getCurrentSong().getTitle()));
    new LibraryUriRequest(mHeadImg,
        getSearchRequestWithAlbumType(MusicServiceRemote.getCurrentSong()),
        new RequestConfig.Builder(IMAGE_SIZE, IMAGE_SIZE).build()).load();
  }

  @Override
  public void onPlayStateChange() {
    super.onPlayStateChange();
    mHeadImg.setBackgroundResource(MusicServiceRemote.isPlaying() && ThemeStore.isLightTheme()
        ? R.drawable.drawer_bg_album_shadow : R.color.transparent);
  }

  @Override
  public void onServiceConnected(@NotNull MusicService service) {
    super.onServiceConnected(service);
  }

  @OnHandleMessage
  public void handleInternal(Message msg) {
    if (msg.what == RECREATE_ACTIVITY) {
      recreate();
    } else if (msg.what == CLEAR_MULTI) {
      for (Fragment temp : getSupportFragmentManager().getFragments()) {
        if (temp instanceof LibraryFragment) {
          ((LibraryFragment) temp).getAdapter().notifyDataSetChanged();
        }
      }
    } else if (msg.what == UPDATE_ADAPTER) {
      //刷新适配器
      for (Fragment temp : getSupportFragmentManager().getFragments()) {
        if (temp instanceof LibraryFragment) {
          ((LibraryFragment) temp).getAdapter().notifyDataSetChanged();
        }
      }
    }
  }

  /**
   * 解析外部打开Intent
   */
  private void parseIntent() {
    if (getIntent() == null) {
      return;
    }
    final Intent intent = getIntent();
    final Uri uri = intent.getData();
    if (uri != null && uri.toString().length() > 0) {
      MusicUtil.playFromUri(uri);
      setIntent(new Intent());
    }
  }

  /**
   * 检查更新
   */

  /**
   * 判断安卓版本，请求安装权限或者直接安装
   *
   * @param activity
   * @param path
   */

}

