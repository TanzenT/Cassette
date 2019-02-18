package tanzent.cassette.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.promeg.pinyinhelper.Pinyin;

import butterknife.BindView;
import io.reactivex.disposables.Disposable;
import tanzent.cassette.App;
import tanzent.cassette.R;
import tanzent.cassette.bean.mp3.Album;
import tanzent.cassette.misc.asynctask.AsynLoadSongNum;
import tanzent.cassette.misc.menu.AlbArtFolderPlaylistListener;
import tanzent.cassette.request.LibraryUriRequest;
import tanzent.cassette.request.RequestConfig;
import tanzent.cassette.theme.Theme;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.ui.MultipleChoice;
import tanzent.cassette.ui.adapter.holder.BaseViewHolder;
import tanzent.cassette.ui.widget.fastcroll_recyclerview.FastScroller;
import tanzent.cassette.util.ColorUtil;
import tanzent.cassette.util.Constants;
import tanzent.cassette.util.DensityUtil;
import tanzent.cassette.util.ImageUriUtil;
import tanzent.cassette.util.SPUtil;
import tanzent.cassette.util.ToastUtil;

import static tanzent.cassette.request.ImageUriRequest.BIG_IMAGE_SIZE;
import static tanzent.cassette.request.ImageUriRequest.SMALL_IMAGE_SIZE;


/**
 * Created by Remix on 2015/12/20.
 */

/**
 * 专辑界面的适配器
 */
public class AlbumAdapter extends HeaderAdapter<Album, BaseViewHolder> implements FastScroller.SectionIndexer {

    public AlbumAdapter(Context context, int layoutId, MultipleChoice multipleChoice) {
        super(context, layoutId, multipleChoice);
        ListModel = SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, "AlbumModel", Constants.GRID_MODEL);
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_topbar_2, parent, false));
        }
        return viewType == Constants.LIST_MODEL ?
                new AlbumListHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album_recycle_list, parent, false)) :
                new AlbumGridHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album_recycle_grid, parent, false));
    }

    @Override
    public void onViewRecycled(BaseViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof AlbumHolder) {
            if (((AlbumHolder) holder).mImage.getTag() != null) {
                Disposable disposable = (Disposable) ((AlbumHolder) holder).mImage.getTag();
                if (!disposable.isDisposed())
                    disposable.dispose();
            }
            ((AlbumHolder) holder).mImage.setImageURI(Uri.EMPTY);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void convert(BaseViewHolder baseHolder, Album album, int position) {
        if (position == 0) {
            final HeaderHolder headerHolder = (HeaderHolder) baseHolder;
            if (mDatas == null || mDatas.size() == 0) {
                headerHolder.mRoot.setVisibility(View.GONE);
                return;
            }
            //设置图标
            headerHolder.mDivider.setVisibility(ListModel == Constants.LIST_MODEL ? View.VISIBLE : View.GONE);
            headerHolder.mListModelBtn.setColorFilter(ListModel == Constants.LIST_MODEL ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
            headerHolder.mGridModelBtn.setColorFilter(ListModel == Constants.GRID_MODEL ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
            headerHolder.mGridModelBtn.setOnClickListener(v -> switchMode(headerHolder, v));
            headerHolder.mListModelBtn.setOnClickListener(v -> switchMode(headerHolder, v));
            return;
        }

        if (!(baseHolder instanceof AlbumHolder)) {
            return;
        }
        final AlbumHolder holder = (AlbumHolder) baseHolder;
        holder.mText1.setText(album.getAlbum());

        //设置封面
        final int albumId = album.getAlbumID();
        final int imageSize = ListModel == 1 ? SMALL_IMAGE_SIZE : BIG_IMAGE_SIZE;

        Disposable disposable = new LibraryUriRequest(holder.mImage, ImageUriUtil.getSearchRequest(album), new RequestConfig.Builder(imageSize, imageSize).build()).load();
        holder.mImage.setTag(disposable);
        if (holder instanceof AlbumListHolder) {
            if (album.getCount() > 0) {
                holder.mText2.setText(mContext.getString(R.string.song_count_2, album.getArtist(), album.getCount()));
            } else {
                new AlbumSongCountLoader(Constants.ALBUM, holder, album).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, album.getAlbumID());
            }
        } else {
            holder.mText2.setText(album.getArtist());
        }

        //背景点击效果
        holder.mContainer.setBackground(
                Theme.getPressAndSelectedStateListRippleDrawable(ListModel, mContext));

        holder.mContainer.setOnClickListener(v -> {
            if (holder.getAdapterPosition() - 1 < 0) {
                ToastUtil.show(mContext, R.string.illegal_arg);
                return;
            }
            mOnItemClickLitener.onItemClick(holder.mContainer, holder.getAdapterPosition() - 1);
        });
        //多选菜单
        holder.mContainer.setOnLongClickListener(v -> {
            if (holder.getAdapterPosition() - 1 < 0) {
                ToastUtil.show(mContext, R.string.illegal_arg);
                return true;
            }
            mOnItemClickLitener.onItemLongClick(holder.mContainer, holder.getAdapterPosition() - 1);
            return true;
        });

        //着色
        int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
        Theme.TintDrawable(holder.mButton, R.drawable.icon_player_more, tintColor);

        //点击效果
        int size = DensityUtil.dip2px(mContext, 45);
        Drawable defaultDrawable = Theme.getShape(ListModel == Constants.LIST_MODEL ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE, Color.TRANSPARENT, size, size);
        Drawable selectDrawable = Theme.getShape(ListModel == Constants.LIST_MODEL ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE, ThemeStore.getSelectColor(), size, size);
        holder.mButton.setBackground(Theme.getPressDrawable(
                defaultDrawable,
                selectDrawable,
                ThemeStore.getRippleColor(),
                null,
                null));

        holder.mButton.setOnClickListener(v -> {
            if (mChoice.isActive())
                return;
            Context wrapper = new ContextThemeWrapper(mContext, Theme.getPopupMenuStyle());
            final PopupMenu popupMenu = new PopupMenu(wrapper, holder.mButton, Gravity.END);
            popupMenu.getMenuInflater().inflate(R.menu.menu_album_item, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new AlbArtFolderPlaylistListener(mContext,
                    albumId,
                    Constants.ALBUM,
                    album.getAlbum()));
            popupMenu.show();
        });

        //是否处于选中状态
        holder.mContainer.setSelected(mChoice.isPositionCheck(position - 1));

        //半圆着色
        if (ListModel == Constants.GRID_MODEL) {
            Theme.TintDrawable(holder.mHalfCircle, R.drawable.icon_half_circular_left,
                    ColorUtil.getColor(ThemeStore.isDay() ? R.color.white : R.color.night_background_color_main));
        }

        //设置padding
        if (ListModel == 2 && holder.mRoot != null) {
            if (position % 2 == 1) {
                holder.mRoot.setPadding(DensityUtil.dip2px(mContext, 6), DensityUtil.dip2px(mContext, 4), DensityUtil.dip2px(mContext, 3), DensityUtil.dip2px(mContext, 4));
            } else {
                holder.mRoot.setPadding(DensityUtil.dip2px(mContext, 3), DensityUtil.dip2px(mContext, 4), DensityUtil.dip2px(mContext, 6), DensityUtil.dip2px(mContext, 4));
            }
        }
    }


    @Override
    public void saveMode() {
        SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, "AlbumModel", ListModel);
    }

    @Override
    public String getSectionText(int position) {
        if (position == 0)
            return "";
        if (mDatas != null && position - 1 < mDatas.size()) {
            String album = mDatas.get(position - 1).getAlbum();
            return !TextUtils.isEmpty(album) ? (Pinyin.toPinyin(album.charAt(0))).toUpperCase().substring(0, 1) : "";
        }
        return "";
    }

    static class AlbumHolder extends BaseViewHolder {
        @BindView(R.id.item_half_circle)
        @Nullable
        ImageView mHalfCircle;
        @BindView(R.id.item_text1)
        TextView mText1;
        @BindView(R.id.item_text2)
        TextView mText2;
        @BindView(R.id.item_button)
        ImageButton mButton;
        @BindView(R.id.item_simpleiview)
        SimpleDraweeView mImage;
        @BindView(R.id.item_container)
        RelativeLayout mContainer;
        @BindView(R.id.item_root)
        @Nullable
        View mRoot;

        AlbumHolder(View v) {
            super(v);
        }
    }

    static class AlbumGridHolder extends AlbumHolder {
        AlbumGridHolder(View v) {
            super(v);
        }
    }

    static class AlbumListHolder extends AlbumHolder {
        AlbumListHolder(View v) {
            super(v);
        }
    }

    static class HeaderHolder extends BaseViewHolder {
        //列表显示与网格显示切换
        @BindView(R.id.list_model)
        ImageButton mListModelBtn;
        @BindView(R.id.grid_model)
        ImageButton mGridModelBtn;
        @BindView(R.id.divider)
        View mDivider;
        View mRoot;

        HeaderHolder(View itemView) {
            super(itemView);
            mRoot = itemView;
        }
    }

    private static class AlbumSongCountLoader extends AsynLoadSongNum {
        private final AlbumHolder mHolder;
        private final Album mAlbum;

        AlbumSongCountLoader(int type, AlbumHolder holder, Album album) {
            super(type);
            mHolder = holder;
            mAlbum = album;
        }

        @Override
        protected void onPostExecute(Integer num) {
            if (mHolder.mText2 != null && num > 0) {
                mAlbum.setCount(num);
                mHolder.mText2.setText(App.getContext().getString(R.string.song_count_2, mAlbum.getArtist(), num));
            }
        }
    }
}
