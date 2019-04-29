package tanzent.cassette.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import butterknife.BindView;
import java.util.List;
import tanzent.cassette.R;
import tanzent.cassette.bean.mp3.Folder;
import tanzent.cassette.misc.asynctask.WrappedAsyncTaskLoader;
import tanzent.cassette.misc.interfaces.LoaderIds;
import tanzent.cassette.misc.interfaces.OnItemClickListener;
import tanzent.cassette.ui.activity.ChildHolderActivity;
import tanzent.cassette.ui.adapter.FolderAdapter;
import tanzent.cassette.util.Constants;
import tanzent.cassette.util.MediaStoreUtil;

/**
 * Created by Remix on 2015/12/5.
 */

/**
 * 文件夹Fragment
 */
public class FolderFragment extends LibraryFragment<Folder, FolderAdapter> {

  @BindView(R.id.recyclerView)
  RecyclerView mRecyclerView;

  public static final String TAG = FolderFragment.class.getSimpleName();

  @Override
  protected int getLayoutID() {
    return R.layout.fragment_folder;
  }

  @Override
  protected void initAdapter() {
    mAdapter = new FolderAdapter(mContext, R.layout.item_folder_recycle, mChoice);
    mAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        Folder folder = mAdapter.getDatas().get(position);
        String path = folder.getPath();
        if (getUserVisibleHint() && !TextUtils.isEmpty(path) &&
            !mChoice.click(position, folder)) {
          ChildHolderActivity.start(mContext, Constants.FOLDER, folder.getParentId(), path);
        }
      }

      @Override
      public void onItemLongClick(View view, int position) {
        Folder folder = mAdapter.getDatas().get(position);
        String path = mAdapter.getDatas().get(position).getPath();
        if (getUserVisibleHint() && !TextUtils.isEmpty(path)) {
          mChoice.longClick(position, folder);
        }
      }
    });
  }

  @Override
  protected void initView() {
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    mRecyclerView.setHasFixedSize(true);
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    mRecyclerView.setAdapter(mAdapter);
  }

  @Override
  protected Loader<List<Folder>> getLoader() {
    return new AsyncFolderLoader(mContext);
  }

  @Override
  protected int getLoaderId() {
    return LoaderIds.FOLDER_FRAGMENT;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPageName = TAG;
  }

  @Override
  public FolderAdapter getAdapter() {
    return mAdapter;
  }

  private static class AsyncFolderLoader extends WrappedAsyncTaskLoader<List<Folder>> {

    private AsyncFolderLoader(Context context) {
      super(context);
    }

    @Override
    public List<Folder> loadInBackground() {
      return MediaStoreUtil.getAllFolder();
    }
  }
}
