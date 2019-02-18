package tanzent.cassette.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import io.reactivex.disposables.Disposable;
import tanzent.cassette.R;
import tanzent.cassette.bean.mp3.Song;
import tanzent.cassette.misc.menu.SongPopupListener;
import tanzent.cassette.request.LibraryUriRequest;
import tanzent.cassette.request.RequestConfig;
import tanzent.cassette.theme.Theme;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.ui.adapter.holder.BaseViewHolder;
import tanzent.cassette.util.ColorUtil;
import tanzent.cassette.util.DensityUtil;

import static tanzent.cassette.request.ImageUriRequest.SMALL_IMAGE_SIZE;
import static tanzent.cassette.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * Created by Remix on 2016/1/23.
 */

/**
 * 搜索结果的适配器
 */
public class SearchAdapter extends BaseAdapter<Song, SearchAdapter.SearchResHolder> {

    private final GradientDrawable mDefaultDrawable;
    private final GradientDrawable mSelectDrawable;

    public SearchAdapter(Context context, int layoutId) {
        super(context, layoutId);
        int size = DensityUtil.dip2px(mContext, 60);
        mDefaultDrawable = Theme.getShape(GradientDrawable.OVAL, Color.TRANSPARENT, size, size);
        mSelectDrawable = Theme.getShape(GradientDrawable.OVAL, ThemeStore.getSelectColor(), size, size);
    }

    @Override
    public void onViewRecycled(SearchAdapter.SearchResHolder holder) {
        super.onViewRecycled(holder);
        if ((holder).mImage.getTag() != null) {
            Disposable disposable = (Disposable) (holder).mImage.getTag();
            if (!disposable.isDisposed())
                disposable.dispose();
        }
        holder.mImage.setImageURI(Uri.EMPTY);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void convert(final SearchResHolder holder, Song song, int position) {
        holder.mName.setText(song.getTitle());
        holder.mOther.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));
        //封面
        Disposable disposable = new LibraryUriRequest(holder.mImage,
                getSearchRequestWithAlbumType(song),
                new RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()).load();
        holder.mImage.setTag(disposable);

        //设置按钮着色
        int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
        Theme.TintDrawable(holder.mButton, R.drawable.icon_player_more, tintColor);

        //按钮点击效果
        holder.mButton.setBackground(Theme.getPressDrawable(
                mDefaultDrawable,
                mSelectDrawable,
                ThemeStore.getRippleColor(),
                null, null));

        holder.mButton.setOnClickListener(v -> {
            Context wrapper = new ContextThemeWrapper(mContext, Theme.getPopupMenuStyle());
            final PopupMenu popupMenu = new PopupMenu(wrapper, holder.mButton, Gravity.END);
            popupMenu.getMenuInflater().inflate(R.menu.menu_song_item, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new SongPopupListener((AppCompatActivity) mContext, song, false, ""));
            popupMenu.show();
        });

        if (mOnItemClickLitener != null && holder.mRooView != null) {
            holder.mRooView.setOnClickListener(v -> mOnItemClickLitener.onItemClick(v, holder.getAdapterPosition()));
        }
    }

    static class SearchResHolder extends BaseViewHolder {
        @BindView(R.id.reslist_item)
        RelativeLayout mRooView;
        @BindView(R.id.search_image)
        SimpleDraweeView mImage;
        @BindView(R.id.search_name)
        TextView mName;
        @BindView(R.id.search_detail)
        TextView mOther;
        @BindView(R.id.search_button)
        ImageButton mButton;

        SearchResHolder(View itemView) {
            super(itemView);
        }
    }
}
