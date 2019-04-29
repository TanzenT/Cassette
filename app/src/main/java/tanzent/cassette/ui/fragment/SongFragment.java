package tanzent.cassette.ui.fragment;


import static tanzent.cassette.helper.MusicServiceRemote.setPlayQueue;
import static tanzent.cassette.service.MusicService.EXTRA_POSITION;
import static tanzent.cassette.util.MusicUtil.makeCmdIntent;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import butterknife.BindView;
import java.util.ArrayList;
import java.util.List;
import tanzent.cassette.R;
import tanzent.cassette.bean.mp3.Song;
import tanzent.cassette.misc.asynctask.WrappedAsyncTaskLoader;
import tanzent.cassette.misc.interfaces.LoaderIds;
import tanzent.cassette.misc.interfaces.OnItemClickListener;
import tanzent.cassette.service.Command;
import tanzent.cassette.ui.adapter.SongAdapter;
import tanzent.cassette.ui.widget.fastcroll_recyclerview.LocationRecyclerView;
import tanzent.cassette.util.MediaStoreUtil;

/**
 * Created by Remix on 2015/11/30.
 */

/**
 * 全部歌曲的Fragment
 */
public class SongFragment extends LibraryFragment<Song, SongAdapter> {

  @BindView(R.id.location_recyclerView)
  LocationRecyclerView mRecyclerView;

  public static final String TAG = SongFragment.class.getSimpleName();

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPageName = TAG;
  }

  @Override
  protected int getLayoutID() {
    return R.layout.fragment_song;
  }

  @Override
  protected void initAdapter() {
    mAdapter = new SongAdapter(mContext, R.layout.item_song_recycle, mChoice, SongAdapter.ALLSONG,
        mRecyclerView);
    mAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        if (getUserVisibleHint() && !mChoice.click(position, mAdapter.getDatas().get(position))) {
          //设置正在播放列表
          final List<Song> songs = mAdapter.getDatas();
          ArrayList<Integer> ids = new ArrayList<>();
          for (Song song : songs) {
            if (song != null && song.getId() > 0) {
              ids.add(song.getId());
            }
          }
          setPlayQueue(ids, makeCmdIntent(Command.PLAYSELECTEDSONG)
              .putExtra(EXTRA_POSITION, position));

        }
      }

      @Override
      public void onItemLongClick(View view, int position) {
        if (getUserVisibleHint()) {
          mChoice.longClick(position, mAdapter.getDatas().get(position));
        }
      }
    });

  }

  @Override
  protected void initView() {
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    mRecyclerView.setAdapter(mAdapter);
    mRecyclerView.setHasFixedSize(true);
  }

  @Override
  protected Loader<List<Song>> getLoader() {
    return new AsyncSongLoader(mContext);
  }

  @Override
  protected int getLoaderId() {
    return LoaderIds.SONG_FRAGMENT;
  }


  @Override
  public SongAdapter getAdapter() {
    return mAdapter;
  }

  @Override
  public void onMetaChanged() {
    if (mAdapter != null) {
      mAdapter.updatePlayingSong();
    }
  }

  public void scrollToCurrent() {
    mRecyclerView.smoothScrollToCurrentSong(mAdapter.getDatas());
  }

  private static class AsyncSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

    private AsyncSongLoader(Context context) {
      super(context);
    }

    @Override
    public List<Song> loadInBackground() {
      return MediaStoreUtil.getAllSong();
    }
  }
}
