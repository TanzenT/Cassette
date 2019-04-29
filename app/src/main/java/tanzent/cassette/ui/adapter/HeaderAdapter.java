package tanzent.cassette.ui.adapter;

import static tanzent.cassette.misc.ExtKt.isPortraitOrientation;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.drawee.view.SimpleDraweeView;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tanzent.cassette.R;
import tanzent.cassette.request.LibraryUriRequest;
import tanzent.cassette.request.RequestConfig;
import tanzent.cassette.request.UriRequest;
import tanzent.cassette.theme.Theme;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.ui.adapter.holder.BaseViewHolder;
import tanzent.cassette.ui.adapter.holder.HeaderHolder;
import tanzent.cassette.ui.misc.MultipleChoice;
import tanzent.cassette.util.ColorUtil;
import tanzent.cassette.util.DensityUtil;
import tanzent.cassette.util.SPUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/17 16:36
 */

public abstract class HeaderAdapter<M, B extends RecyclerView.ViewHolder> extends
    BaseAdapter<M, BaseViewHolder> {

  // 封面uri缓存
  SparseArray<String> mUriCache = new SparseArray<>();

  //显示模式 1:列表 2:网格
  public final static int LIST_MODE = 1;
  public final static int GRID_MODE = 2;

  //网格模式下水平和垂直的间距 以间距当作Divider
  private static final int GRID_MARGIN_VERTICAL = DensityUtil.dip2px(4);
  private static final int GRID_MARGIN_HORIZONTAL = DensityUtil.dip2px(6);

  static final int TYPE_HEADER = 0;
  static final int TYPE_NORMAL = 1;

  protected MultipleChoice mChoice;
  protected RecyclerView mRecyclerView;

  //当前列表模式 1:列表 2:网格
  int mMode = GRID_MODE;

  HeaderAdapter(Context context, int layoutId, MultipleChoice multiChoice,
      RecyclerView recyclerView) {
    super(context, layoutId);
    this.mChoice = multiChoice;
    this.mRecyclerView = recyclerView;
    String key = this instanceof AlbumAdapter ? SPUtil.SETTING_KEY.MODE_FOR_ALBUM :
        this instanceof ArtistAdapter ? SPUtil.SETTING_KEY.MODE_FOR_ARTIST :
            this instanceof PlayListAdapter ? SPUtil.SETTING_KEY.MODE_FOR_PLAYLIST :
                null;
    //其他的列表都是List模式
    this.mMode =
        key != null ? SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, key, GRID_MODE) : LIST_MODE;
  }

  @Override
  public int getItemViewType(int position) {
    if (position == 0) {
      return TYPE_HEADER;
    }
    return mMode;
  }

  @Override
  protected M getItem(int position) {
    return mDatas != null ? position == 0 ? null
        : position - 1 < mDatas.size() ? mDatas.get(position - 1) : null : null;
  }

  @Override
  public int getItemCount() {
    return mDatas != null ? super.getItemCount() + 1 : 0;
  }

  @Override
  public void onAttachedToRecyclerView(RecyclerView recyclerView) {
    super.onAttachedToRecyclerView(recyclerView);
    RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
    if (manager instanceof GridLayoutManager) {
      final GridLayoutManager gridManager = ((GridLayoutManager) manager);
      gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
          return getItemViewType(position) == TYPE_HEADER ? gridManager.getSpanCount() : 1;
        }
      });
    }
  }


  /**
   * 初始化列表模式切换的按钮
   */
  void setUpModeButton(HeaderHolder headerHolder) {
    if (mDatas == null || mDatas.size() == 0) {
      headerHolder.mRoot.setVisibility(View.GONE);
      return;
    }
    //设置图标

    headerHolder.mDivider
        .setVisibility(mMode == HeaderAdapter.LIST_MODE ? View.VISIBLE : View.GONE);
    headerHolder.mGridModeBtn.setOnClickListener(v -> switchMode(headerHolder, v));
    headerHolder.mListModeBtn.setOnClickListener(v -> switchMode(headerHolder, v));
    headerHolder.mDivider.setVisibility(mMode == LIST_MODE ? View.VISIBLE : View.GONE);
    tintModeButton(headerHolder);
  }

  /**
   * 列表模式切换
   */
  private void switchMode(HeaderHolder headerHolder, View v) {
    int newModel = v.getId() == R.id.list_model ? LIST_MODE : GRID_MODE;
    if (newModel == mMode) {
      return;
    }
    mMode = newModel;
    setUpModeButton(headerHolder);
    //重新设置LayoutManager和adapter并刷新列表
    mRecyclerView.setLayoutManager(mMode == LIST_MODE ? new LinearLayoutManager(mContext)
        : new GridLayoutManager(mContext, 2));
    mRecyclerView.setAdapter(this);
    //保存当前模式
    saveMode();
  }

  private void tintModeButton(HeaderHolder headerHolder) {
    headerHolder.mListModeBtn.setImageDrawable(
        Theme.tintVectorDrawable(mContext, R.drawable.ic_format_list_bulleted_white_24dp,
            mMode == LIST_MODE ? ThemeStore.getAccentColor()
                : ColorUtil.getColor(R.color.default_model_button_color))
    );

    headerHolder.mGridModeBtn.setImageDrawable(
        Theme.tintVectorDrawable(mContext, R.drawable.ic_apps_white_24dp,
            mMode == GRID_MODE ? ThemeStore.getAccentColor()
                : ColorUtil.getColor(R.color.default_model_button_color))
    );

  }

  private void saveMode() {
    String key = this instanceof AlbumAdapter ? SPUtil.SETTING_KEY.MODE_FOR_ALBUM :
        this instanceof ArtistAdapter ? SPUtil.SETTING_KEY.MODE_FOR_ARTIST :
            SPUtil.SETTING_KEY.MODE_FOR_PLAYLIST;
    SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, key, mMode);
  }

  void setMarginForGridLayout(BaseViewHolder holder, int position) {
    //设置margin,当作Divider
    if (mMode == GRID_MODE && holder.mRoot != null) {
      ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.mRoot
          .getLayoutParams();
      if (isPortraitOrientation(mContext)) { //竖屏
        if (position % 2 == 1) {
          lp.setMargins(GRID_MARGIN_HORIZONTAL, GRID_MARGIN_VERTICAL,
              GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL);
        } else {
          lp.setMargins(GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL,
              GRID_MARGIN_HORIZONTAL, GRID_MARGIN_VERTICAL);
        }
      } else { //横屏
        lp.setMargins(GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL / 2,
            GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL / 2);
      }

      holder.mRoot.setLayoutParams(lp);
    }
  }

  Disposable setImage(final SimpleDraweeView simpleDraweeView,
      final UriRequest uriRequest,
      final int imageSize,
      final int position) {
    final String cacheUri = mUriCache.get(position);
    if (cacheUri != null) {
      simpleDraweeView.setImageURI(mUriCache.get(position));
      return Observable.just("").subscribe();
    } else {
      return new LibraryUriRequest(simpleDraweeView,
          uriRequest,
          new RequestConfig.Builder(imageSize, imageSize).build()) {
        @Override
        public void onSave(@NotNull String result) {
          super.onSave(result);
          mUriCache.put(position, result);
        }

        @Override
        public void onError(@Nullable Throwable throwable) {
          super.onError(throwable);
          if (throwable == null) {
            // 没有错误类型 忽略
          } else if (throwable instanceof UnknownHostException) {
            // 没有网络 忽略
          } else if(throwable instanceof NoSuchElementException){
            // 没有结果不再查找
            mUriCache.put(position,"");
          } else if (ERROR_NO_RESULT.equals(throwable.getMessage()) ||
              ERROR_BLACKLIST.equals(throwable.getMessage())) {
            // 黑名单或者没有结果 不再查找
            mUriCache.put(position, "");
          } else {
            // 默认不处理
          }
        }
      }.load();
    }
  }

  public void clearUriCache() {
    mUriCache.clear();
  }

  void disposeLoad(final ViewHolder holder) {
    //
    final ViewGroup parent = holder.itemView instanceof ViewGroup ? (ViewGroup) holder.itemView : null;
    if (parent != null) {
      for (int i = 0; i < parent.getChildCount(); i++) {
        final View childView = parent.getChildAt(i);
        if (childView instanceof SimpleDraweeView) {
          final Object tag = childView.getTag();
          if (tag != null && tag instanceof Disposable) {
            Disposable disposable = (Disposable) tag;
            if (!disposable.isDisposed()) {
              disposable.dispose();
            }
            childView.setTag(null);
          }
        }
      }
    }
  }

}
