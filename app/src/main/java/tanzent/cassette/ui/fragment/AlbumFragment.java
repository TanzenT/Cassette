package tanzent.cassette.ui.fragment;

import static tanzent.cassette.ui.adapter.HeaderAdapter.LIST_MODE;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import butterknife.BindView;
import java.util.List;
import tanzent.cassette.R;
import tanzent.cassette.bean.mp3.Album;
import tanzent.cassette.misc.asynctask.WrappedAsyncTaskLoader;
import tanzent.cassette.misc.interfaces.LoaderIds;
import tanzent.cassette.misc.interfaces.OnItemClickListener;
import tanzent.cassette.ui.activity.ChildHolderActivity;
import tanzent.cassette.ui.adapter.AlbumAdapter;
import tanzent.cassette.ui.adapter.HeaderAdapter;
import tanzent.cassette.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import tanzent.cassette.util.Constants;
import tanzent.cassette.util.MediaStoreUtil;
import tanzent.cassette.util.SPUtil;

/**
 * Created by Remix on 2015/12/20.
 */

/**
 * 专辑Fragment
 */
public class AlbumFragment extends LibraryFragment<Album, AlbumAdapter> {

  @BindView(R.id.recyclerView)
  FastScrollRecyclerView mRecyclerView;

  public static final String TAG = AlbumFragment.class.getSimpleName();

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPageName = TAG;
  }

  @Override
  protected int getLayoutID() {
    return R.layout.fragment_album;
  }

  @Override
  protected void initAdapter() {
    mAdapter = new AlbumAdapter(mContext, R.layout.item_album_recycle_grid, mChoice, mRecyclerView);
    mAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        Album album = mAdapter.getDatas().get(position);
        if (getUserVisibleHint() && !mChoice.click(position, album)) {
          ChildHolderActivity
              .start(mContext, Constants.ALBUM, album.getAlbumID(), album.getAlbum());
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
    int mode = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MODE_FOR_ALBUM,
        HeaderAdapter.GRID_MODE);
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    mRecyclerView.setLayoutManager(
        mode == LIST_MODE ? new LinearLayoutManager(mContext) : new GridLayoutManager(mContext, getSpanCount()));
    mRecyclerView.setAdapter(mAdapter);
    mRecyclerView.setHasFixedSize(true);
  }


  @Override
  public AlbumAdapter getAdapter() {
    return mAdapter;
  }

  @Override
  protected Loader<List<Album>> getLoader() {
    return new AsyncAlbumLoader(mContext);
  }

  @Override
  protected int getLoaderId() {
    return LoaderIds.ALBUM_FRAGMENT;
  }

  private static class AsyncAlbumLoader extends WrappedAsyncTaskLoader<List<Album>> {

    private AsyncAlbumLoader(Context context) {
      super(context);
    }

    @Override
    public List<Album> loadInBackground() {
      return MediaStoreUtil.getAllAlbum();
    }
  }
}
