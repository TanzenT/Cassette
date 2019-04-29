package tanzent.cassette.ui.activity;

import static tanzent.cassette.helper.MusicServiceRemote.setPlayQueue;
import static tanzent.cassette.service.MusicService.EXTRA_POSITION;
import static tanzent.cassette.util.MusicUtil.makeCmdIntent;
import static tanzent.cassette.util.Util.registerLocalReceiver;
import static tanzent.cassette.util.Util.unregisterLocalReceiver;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.facebook.drawee.backends.pipeline.Fresco;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import tanzent.cassette.R;
import tanzent.cassette.bean.mp3.Song;
import tanzent.cassette.db.room.DatabaseRepository;
import tanzent.cassette.db.room.model.PlayList;
import tanzent.cassette.helper.SortOrder;
import tanzent.cassette.misc.asynctask.AppWrappedAsyncTaskLoader;
import tanzent.cassette.misc.handler.MsgHandler;
import tanzent.cassette.misc.handler.OnHandleMessage;
import tanzent.cassette.misc.interfaces.LoaderIds;
import tanzent.cassette.misc.interfaces.OnItemClickListener;
import tanzent.cassette.misc.interfaces.OnTagEditListener;
import tanzent.cassette.misc.tageditor.TagReceiver;
import tanzent.cassette.request.UriRequest;
import tanzent.cassette.service.Command;
import tanzent.cassette.service.MusicService;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.ui.adapter.ChildHolderAdapter;
import tanzent.cassette.ui.misc.MultipleChoice;
import tanzent.cassette.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import tanzent.cassette.util.ColorUtil;
import tanzent.cassette.util.Constants;
import tanzent.cassette.util.ImageUriUtil;
import tanzent.cassette.util.MediaStoreUtil;
import tanzent.cassette.util.SPUtil;
import tanzent.cassette.util.ToastUtil;

/**
 * Created by Remix on 2015/12/4.
 */

/**
 * 专辑、艺术家、文件夹、播放列表详情
 */
public class ChildHolderActivity extends LibraryActivity<Song, ChildHolderAdapter>
    implements OnTagEditListener {

  public final static String TAG = ChildHolderActivity.class.getSimpleName();
  //获得歌曲信息列表的参数
  private int mId;
  private int mType;
  private String mArg;
  private TagReceiver mTagEditReceiver;

  //歌曲数目与标题
  @BindView(R.id.childholder_item_num)
  TextView mNum;
  @BindView(R.id.child_holder_recyclerView)
  FastScrollRecyclerView mRecyclerView;
  @BindView(R.id.toolbar)
  Toolbar mToolBar;

  private String Title;
//    private MaterialDialog mMDDialog;

  //当前排序
  private String mSortOrder;
  private MsgHandler mRefreshHandler;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_child_holder);
    ButterKnife.bind(this);

    mRefreshHandler = new MsgHandler(this);
    mTagEditReceiver = new TagReceiver(this);
    registerLocalReceiver(mTagEditReceiver, new IntentFilter(TagReceiver.ACTION_EDIT_TAG));

    //参数id，类型，标题
    mId = getIntent().getIntExtra(EXTRA_ID, -1);
    mType = getIntent().getIntExtra(EXTRA_TYPE, -1);
    mArg = getIntent().getStringExtra(EXTRA_TITLE);
    if (mId == -1 || mType == -1 || TextUtils.isEmpty(mArg)) {
      ToastUtil.show(this, R.string.illegal_arg);
      finish();
    }

    mChoice = new MultipleChoice<>(this,
        mType == Constants.PLAYLIST ? Constants.PLAYLISTSONG : Constants.SONG);

    mAdapter = new ChildHolderAdapter(this, R.layout.item_child_holder, mType, mArg, mChoice,
        mRecyclerView);
    mChoice.setAdapter(mAdapter);
    mChoice.setExtra(mId);
    mAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        final Song song = mAdapter.getDatas().get(position);
        if (!mChoice.click(position, song)) {
          final List<Song> songs = mAdapter.getDatas();
          if (songs.size() == 0) {
            return;
          }
          ArrayList<Integer> idList = new ArrayList<>();
          for (Song info : songs) {
            if (info != null && info.getId() > 0) {
              idList.add(info.getId());
            }
          }
          //设置正在播放列表
          setPlayQueue(idList, makeCmdIntent(Command.PLAYSELECTEDSONG)
              .putExtra(EXTRA_POSITION, position));
        }
      }

      @Override
      public void onItemLongClick(View view, int position) {
        mChoice.longClick(position, mAdapter.getDatas().get(position));
      }
    });

    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    mRecyclerView.setAdapter(mAdapter);
    final int accentColor = ThemeStore.getAccentColor();
    mRecyclerView.setBubbleColor(accentColor);
    mRecyclerView.setHandleColor(accentColor);
    mRecyclerView.setBubbleTextColor(ColorUtil.getColor(ColorUtil.isColorLight(accentColor) ?
        R.color.dark_text_color_primary : R.color.light_text_color_primary));

    //标题
    if (mType != Constants.FOLDER) {
      if (mArg.contains("unknown")) {
        if (mType == Constants.ARTIST) {
          Title = getString(R.string.unknown_artist);
        } else if (mType == Constants.ALBUM) {
          Title = getString(R.string.unknown_album);
        }
      } else {
        Title = mArg;
      }
    } else {
      Title = mArg.substring(mArg.lastIndexOf("/") + 1, mArg.length());
    }
    //初始化toolbar
    setUpToolbar(Title);

//        mMDDialog = new MaterialDialog.Builder(this)
//                .title(R.string.loading)
//                .titleColorAttr(R.attr.text_color_primary)
//                .content(R.string.please_wait)
//                .contentColorAttr(R.attr.text_color_primary)
//                .progress(true, 0)
//                .backgroundColorAttr(R.attr.background_color_3)
//                .progressIndeterminateStyle(false).build();
  }

  @Override
  public boolean onCreateOptionsMenu(@NonNull Menu menu) {
    super.onCreateOptionsMenu(menu);
    if (mType == Constants.PLAYLIST) {
      mSortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME,
          SPUtil.SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER,
          SortOrder.PlayListSongSortOrder.SONG_A_Z);
    } else if (mType == Constants.ALBUM) {
      mSortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME,
          SPUtil.SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER,
          SortOrder.ChildHolderSongSortOrder.SONG_TRACK_NUMBER);
    } else if (mType == Constants.ARTIST) {
      mSortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME,
          SPUtil.SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER,
          SortOrder.ChildHolderSongSortOrder.SONG_A_Z);
    } else {
      mSortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME,
          SPUtil.SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER,
          SortOrder.ChildHolderSongSortOrder.SONG_A_Z);
    }
    if (TextUtils.isEmpty(mSortOrder)) {
      return true;
    }
    setUpMenuItem(menu, mSortOrder);
    return true;
  }

  @Override
  protected void saveSortOrder(String sortOrder) {
    boolean update = false;
    if (mType == Constants.PLAYLIST) {
      //手动排序或者排序发生变化
      if (sortOrder.equalsIgnoreCase(SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM) ||
          !mSortOrder.equalsIgnoreCase(sortOrder)) {
        //选择的是手动排序
        if (sortOrder.equalsIgnoreCase(SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM)) {
          CustomSortActivity.start(mContext, mId, mArg, new ArrayList<>(mAdapter.getDatas()));
        } else {
          update = true;
        }
      }
    } else {
      //排序发生变化
      if (!mSortOrder.equalsIgnoreCase(sortOrder)) {
        update = true;
      }
    }
    if (mType == Constants.PLAYLIST) {
      SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME,
          SPUtil.SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER, sortOrder);
    } else if (mType == Constants.ALBUM) {
      SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME,
          SPUtil.SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER, sortOrder);
    } else if (mType == Constants.ARTIST) {
      SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME,
          SPUtil.SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER, sortOrder);
    } else {
      SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME,
          SPUtil.SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER, sortOrder);
    }
    mSortOrder = sortOrder;
    if (update) {
      onMediaStoreChanged();
    }

  }


  @Override
  public int getMenuLayoutId() {
    return mType == Constants.PLAYLIST ? R.menu.menu_child_for_playlist :
        mType == Constants.ALBUM ? R.menu.menu_child_for_album :
            mType == Constants.ARTIST ? R.menu.menu_child_for_artist : R.menu.menu_child_for_folder;
  }

  @Override
  protected int getLoaderId() {
    return LoaderIds.CHILDHOLDER_ACTIVITY;
  }


  @Override
  public void onServiceConnected(@NotNull MusicService service) {
    super.onServiceConnected(service);
  }

  @Override
  public void onPlayListChanged(String name) {
    super.onPlayListChanged(name);
    if (name.equals(PlayList.TABLE_NAME)) {
      onMediaStoreChanged();
    }
  }

  public void onTagEdit(Song newSong) {
    if (newSong == null) {
      return;
    }
    Fresco.getImagePipeline().clearCaches();
    final UriRequest request = ImageUriUtil.getSearchRequestWithAlbumType(newSong);
    SPUtil.deleteValue(mContext, SPUtil.COVER_KEY.NAME, request.getLastFMKey());
    SPUtil.deleteValue(mContext, SPUtil.COVER_KEY.NAME, request.getNeteaseCacheKey());
    if (mType == Constants.ARTIST || mType == Constants.ALBUM) {
      mId = mType == Constants.ARTIST ? newSong.getArtistId() : newSong.getAlbumId();
      Title = mType == Constants.ARTIST ? newSong.getArtist() : newSong.getAlbum();
      mToolBar.setTitle(Title);
      onMediaStoreChanged();
    }
  }

  /**
   * 根据参数(专辑id 歌手id 文件夹名 播放列表名)获得对应的歌曲信息列表
   *
   * @return 对应歌曲信息列表
   */
  @WorkerThread
  private List<Song> getMP3List() {
    if (mId < 0) {
      return null;
    }
    switch (mType) {
      //专辑id
      case Constants.ALBUM:
        return MediaStoreUtil.getSongsByArtistIdOrAlbumId(mId, Constants.ALBUM);
      //歌手id
      case Constants.ARTIST:
        return MediaStoreUtil.getSongsByArtistIdOrAlbumId(mId, Constants.ARTIST);
      //文件夹名
      case Constants.FOLDER:
        return MediaStoreUtil.getSongsByParentId(mId);
      //播放列表名
      case Constants.PLAYLIST:
        /* 播放列表歌曲id列表 */
        return DatabaseRepository.getInstance()
            .getPlayList(mId)
            .flatMap((Function<PlayList, SingleSource<List<Song>>>) playList ->
                DatabaseRepository.getInstance()
                    .getPlayListSongs(mContext, playList, false))
            .blockingGet();
    }
    return new ArrayList<>();
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mRefreshHandler.remove();
    unregisterLocalReceiver(mTagEditReceiver);
  }

  @OnHandleMessage
  public void handleInternal(Message msg) {
    switch (msg.what) {
      case CLEAR_MULTI:
        mAdapter.notifyDataSetChanged();
        break;
//            case START:
//                if (mMDDialog != null && !mMDDialog.isShowing()) {
//                    mMDDialog.show();
//                }
//                break;
//            case END:
//                if (mMDDialog != null && mMDDialog.isShowing()) {
//                    mMDDialog.dismiss();
//                }
//                break;
    }
  }

  @Override
  public void onLoadFinished(Loader<List<Song>> loader, List<Song> data) {
    super.onLoadFinished(loader, data);
    mNum.setText(getString(R.string.song_count, data != null ? data.size() : 0));
  }

  @Override
  protected Loader<List<Song>> getLoader() {
    return new AsyncChildSongLoader(this);
  }

  @Override
  public void onMetaChanged() {
    super.onMetaChanged();
    if (mAdapter != null) {
      mAdapter.updatePlayingSong();
    }
  }

  private static final String EXTRA_ID = "id";
  private static final String EXTRA_TYPE = "type";
  private static final String EXTRA_TITLE = "title";

  public static void start(Context context, int type, int id, String title) {
    context.startActivity(new Intent(context, ChildHolderActivity.class)
        .putExtra(EXTRA_ID, id)
        .putExtra(EXTRA_TYPE, type)
        .putExtra(EXTRA_TITLE, title));
  }

  private static class AsyncChildSongLoader extends AppWrappedAsyncTaskLoader<List<Song>> {

    private final WeakReference<ChildHolderActivity> mRef;

    private AsyncChildSongLoader(ChildHolderActivity childHolderActivity) {
      super(childHolderActivity);
      mRef = new WeakReference<>(childHolderActivity);
    }

    @Override
    public List<Song> loadInBackground() {
      return getChildSongs();
    }

    @NonNull
    private List<Song> getChildSongs() {
      ChildHolderActivity activity = mRef.get();
      List<Song> songs = activity.getMP3List();
      return songs != null ? songs : new ArrayList<>();
    }
  }

}