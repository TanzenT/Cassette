package tanzent.cassette.ui.activity;

import static tanzent.cassette.helper.MusicServiceRemote.setPlayQueue;
import static tanzent.cassette.service.MusicService.EXTRA_POSITION;
import static tanzent.cassette.util.MusicUtil.makeCmdIntent;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.util.ArrayList;
import java.util.List;
import tanzent.cassette.R;
import tanzent.cassette.bean.mp3.Song;
import tanzent.cassette.misc.asynctask.AppWrappedAsyncTaskLoader;
import tanzent.cassette.misc.handler.MsgHandler;
import tanzent.cassette.misc.handler.OnHandleMessage;
import tanzent.cassette.misc.interfaces.LoaderIds;
import tanzent.cassette.misc.interfaces.OnItemClickListener;
import tanzent.cassette.service.Command;
import tanzent.cassette.ui.adapter.SongAdapter;
import tanzent.cassette.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import tanzent.cassette.util.MediaStoreUtil;

/**
 * Created by taeja on 16-3-4.
 */

/**
 * 最近添加歌曲的界面 目前为最近7天添加
 */
public class RecentlyActivity extends LibraryActivity<Song, SongAdapter> {

  public static final String TAG = RecentlyActivity.class.getSimpleName();

  @BindView(R.id.recently_placeholder)
  View mPlaceHolder;
  @BindView(R.id.recyclerview)
  FastScrollRecyclerView mRecyclerView;
  private ArrayList<Integer> mIdList = new ArrayList<>();

  private MsgHandler mHandler;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_recently);
    ButterKnife.bind(this);

    mHandler = new MsgHandler(this);

    mAdapter = new SongAdapter(this, R.layout.item_song_recycle, mChoice, SongAdapter.RECENTLY,
        mRecyclerView);
    mChoice.setAdapter(mAdapter);
    mAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        final Song song = mAdapter.getDatas().get(position);
        if (song != null && !mChoice.click(position, song)) {
          setPlayQueue(mIdList, makeCmdIntent(Command.PLAYSELECTEDSONG)
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

    setUpToolbar(getString(R.string.recently));
  }

  @Override
  public void onLoadFinished(android.content.Loader<List<Song>> loader, List<Song> data) {
    super.onLoadFinished(loader, data);
    if (data != null) {
      mIdList = new ArrayList<>();
      for (Song song : data) {
        mIdList.add(song.getId());
      }
      mRecyclerView.setVisibility(data.size() > 0 ? View.VISIBLE : View.GONE);
      mPlaceHolder.setVisibility(data.size() > 0 ? View.GONE : View.VISIBLE);
    } else {
      mRecyclerView.setVisibility(View.GONE);
      mPlaceHolder.setVisibility(View.VISIBLE);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @OnHandleMessage
  public void handleMessage(Message msg) {
    switch (msg.what) {
      case CLEAR_MULTI:
        if (mAdapter != null) {
          mAdapter.notifyDataSetChanged();
        }
        break;
      case UPDATE_ADAPTER:
        if (mAdapter != null) {
          mAdapter.notifyDataSetChanged();
        }
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mHandler.remove();
  }

  @Override
  public void onMediaStoreChanged() {
    super.onMediaStoreChanged();
    if(mAdapter != null){
      mAdapter.clearUriCache();
    }
  }

  @Override
  protected android.content.Loader<List<Song>> getLoader() {
    return new AsyncRecentlySongLoader(this);
  }

  @Override
  protected int getLoaderId() {
    return LoaderIds.RECENTLY_ACTIVITY;
  }

  private static class AsyncRecentlySongLoader extends AppWrappedAsyncTaskLoader<List<Song>> {

    private AsyncRecentlySongLoader(Context context) {
      super(context);
    }

    @Override
    public List<Song> loadInBackground() {
      return getLastAddedSongs();
    }

    @NonNull
    private List<Song> getLastAddedSongs() {
      return MediaStoreUtil.getLastAddedSong();
    }
  }
}
