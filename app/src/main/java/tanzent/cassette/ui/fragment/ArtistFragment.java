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
import tanzent.cassette.bean.mp3.Artist;
import tanzent.cassette.misc.asynctask.WrappedAsyncTaskLoader;
import tanzent.cassette.misc.interfaces.LoaderIds;
import tanzent.cassette.misc.interfaces.OnItemClickListener;
import tanzent.cassette.ui.activity.ChildHolderActivity;
import tanzent.cassette.ui.adapter.ArtistAdapter;
import tanzent.cassette.ui.adapter.HeaderAdapter;
import tanzent.cassette.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import tanzent.cassette.util.Constants;
import tanzent.cassette.util.MediaStoreUtil;
import tanzent.cassette.util.SPUtil;

/**
 * Created by Remix on 2015/12/22.
 */

/**
 * 艺术家Fragment
 */
public class ArtistFragment extends LibraryFragment<Artist, ArtistAdapter> {

  @BindView(R.id.recyclerView)
  FastScrollRecyclerView mRecyclerView;

  public static final String TAG = ArtistFragment.class.getSimpleName();

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPageName = TAG;
  }

  @Override
  protected int getLayoutID() {
    return R.layout.fragment_artist;
  }

  @Override
  protected void initAdapter() {
    mAdapter = new ArtistAdapter(mContext, R.layout.item_artist_recycle_grid, mChoice,
        mRecyclerView);
    mAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        final Artist artist = mAdapter.getDatas().get(position);
        if (getUserVisibleHint() && artist != null &&
            !mChoice.click(position, artist)) {
          if (mAdapter.getDatas() != null) {
            ChildHolderActivity
                .start(mContext, Constants.ARTIST, artist.getArtistID(), artist.getArtist());
          }
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
    int model = SPUtil
        .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MODE_FOR_ARTIST,
            HeaderAdapter.GRID_MODE);
    mRecyclerView.setLayoutManager(model == LIST_MODE ? new LinearLayoutManager(mContext)
        : new GridLayoutManager(getActivity(), getSpanCount()));
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    mRecyclerView.setAdapter(mAdapter);
    mRecyclerView.setHasFixedSize(true);
  }

  @Override
  protected Loader<List<Artist>> getLoader() {
    return new AsyncArtistLoader(mContext);
  }

  @Override
  protected int getLoaderId() {
    return LoaderIds.ARTIST_FRAGMENT;
  }

  @Override
  public ArtistAdapter getAdapter() {
    return mAdapter;
  }


  private static class AsyncArtistLoader extends WrappedAsyncTaskLoader<List<Artist>> {

    private AsyncArtistLoader(Context context) {
      super(context);
    }

    @Override
    public List<Artist> loadInBackground() {
      return MediaStoreUtil.getAllArtist();
    }
  }

}
