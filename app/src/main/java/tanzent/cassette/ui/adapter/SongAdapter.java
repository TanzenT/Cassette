package tanzent.cassette.ui.adapter;

import static tanzent.cassette.request.ImageUriRequest.SMALL_IMAGE_SIZE;
import static tanzent.cassette.theme.ThemeStore.getHighLightTextColor;
import static tanzent.cassette.theme.ThemeStore.getTextColorPrimary;
import static tanzent.cassette.util.ImageUriUtil.getSearchRequestWithAlbumType;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.github.promeg.pinyinhelper.Pinyin;
import java.util.ArrayList;
import java.util.List;
import tanzent.cassette.R;
import tanzent.cassette.bean.mp3.Song;
import tanzent.cassette.helper.MusicServiceRemote;
import tanzent.cassette.misc.menu.SongPopupListener;
import tanzent.cassette.service.Command;
import tanzent.cassette.theme.Theme;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.ui.adapter.holder.BaseViewHolder;
import tanzent.cassette.ui.misc.MultipleChoice;
import tanzent.cassette.ui.widget.fastcroll_recyclerview.FastScroller;
import tanzent.cassette.util.MusicUtil;
import tanzent.cassette.util.ToastUtil;

/**
 * 全部歌曲和最近添加页面所用adapter
 */

/**
 * Created by Remix on 2016/4/11.
 */
public class SongAdapter extends HeaderAdapter<Song, BaseViewHolder> implements FastScroller.SectionIndexer {

  private int mType;
  public static final int ALLSONG = 0;
  public static final int RECENTLY = 1;

  private Song mLastPlaySong = MusicServiceRemote.getCurrentSong();

  public SongAdapter(Context context, int layoutId, MultipleChoice multiChoice, int type,
      RecyclerView recyclerView) {
    super(context, layoutId, multiChoice, recyclerView);
    mType = type;
    mRecyclerView = recyclerView;
  }

  @Override
  public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return viewType == TYPE_HEADER ?
        new HeaderHolder(
            LayoutInflater.from(mContext).inflate(R.layout.layout_header_1, parent, false)) :
        new SongViewHolder(
            LayoutInflater.from(mContext).inflate(R.layout.item_song_recycle, parent, false));
  }

  @Override
  public void onViewRecycled(BaseViewHolder holder) {
    super.onViewRecycled(holder);
    disposeLoad(holder);
//    if (holder instanceof SongViewHolder) {
//      final SongViewHolder songViewHolder = (SongViewHolder) holder;
//      if (songViewHolder.mImage.getTag() != null) {
//        Disposable disposable = (Disposable) songViewHolder.mImage.getTag();
//        if (!disposable.isDisposed()) {
//          disposable.dispose();
//        }
//        songViewHolder.mImage.setTag(null);
//      }
//    }
  }

  @SuppressLint("RestrictedApi")
  @Override
  protected void convert(BaseViewHolder baseHolder, final Song song, int position) {
    if (position == 0) {
      final HeaderHolder headerHolder = (HeaderHolder) baseHolder;
      //没有歌曲时隐藏
      if (mDatas == null || mDatas.size() == 0) {
        headerHolder.mRoot.setVisibility(View.GONE);
        return;
      } else {
        headerHolder.mRoot.setVisibility(View.VISIBLE);
      }

      headerHolder.mShuffleIv.setImageDrawable(
          Theme.tintVectorDrawable(mContext, R.drawable.ic_shuffle_white_24dp,
              ThemeStore.getAccentColor())
      );

      headerHolder.mRoot.setOnClickListener(v -> {
        Intent intent = MusicUtil.makeCmdIntent(Command.NEXT, true);
        if (mType == ALLSONG) {
          List<Integer> allSong = MusicServiceRemote.getAllSong();
          if (allSong == null || allSong.isEmpty()) {
            ToastUtil.show(mContext, R.string.no_song);
            return;
          }
          MusicServiceRemote.setPlayQueue(allSong, intent);
        } else {
          ArrayList<Integer> IdList = new ArrayList<>();
          for (int i = 0; i < mDatas.size(); i++) {
            IdList.add(mDatas.get(i).getId());
          }
          if (IdList.size() == 0) {
            ToastUtil.show(mContext, R.string.no_song);
            return;
          }
          MusicServiceRemote.setPlayQueue(IdList, intent);
        }
      });
      return;
    }

    if (!(baseHolder instanceof SongViewHolder)) {
      return;
    }
    final SongViewHolder holder = (SongViewHolder) baseHolder;

    //封面
    holder.mImage.setTag(setImage(holder.mImage,getSearchRequestWithAlbumType(song),SMALL_IMAGE_SIZE, position));

//        //是否为无损
//        if(!TextUtils.isEmpty(song.getDisplayname())){
//            String prefix = song.getDisplayname().substring(song.getDisplayname().lastIndexOf(".") + 1);
//            holder.mSQ.setVisibility(prefix.equals("flac") || prefix.equals("ape") || prefix.equals("wav")? View.VISIBLE : View.GONE);
//        }

    //高亮
    if (MusicServiceRemote.getCurrentSong().getId() == song.getId()) {
      mLastPlaySong = song;
      holder.mName.setTextColor(getHighLightTextColor());
      holder.mIndicator.setVisibility(View.VISIBLE);
    } else {
      holder.mName.setTextColor(getTextColorPrimary());
      holder.mIndicator.setVisibility(View.GONE);
    }
    holder.mIndicator.setBackgroundColor(getHighLightTextColor());

    //标题
    holder.mName.setText(song.getShowName());

    //艺术家与专辑
    holder.mOther.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));

    //设置按钮着色
    int tintColor = ThemeStore.getLibraryBtnColor();
    Theme.tintDrawable(holder.mButton, R.drawable.icon_player_more, tintColor);

    holder.mButton.setOnClickListener(v -> {
      if (mChoice.isActive()) {
        return;
      }
      final PopupMenu popupMenu = new PopupMenu(mContext, holder.mButton, Gravity.END);
      popupMenu.getMenuInflater().inflate(R.menu.menu_song_item, popupMenu.getMenu());
      popupMenu.setOnMenuItemClickListener(
          new SongPopupListener((AppCompatActivity) mContext, song, false, ""));
      popupMenu.show();
    });

    holder.mContainer.setOnClickListener(v -> {
      if (holder.getAdapterPosition() - 1 < 0) {
        ToastUtil.show(mContext, R.string.illegal_arg);
        return;
      }
      mOnItemClickListener.onItemClick(v, holder.getAdapterPosition() - 1);
    });
    holder.mContainer.setOnLongClickListener(v -> {
      if (holder.getAdapterPosition() - 1 < 0) {
        ToastUtil.show(mContext, R.string.illegal_arg);
        return true;
      }
      mOnItemClickListener.onItemLongClick(v, holder.getAdapterPosition() - 1);
      return true;
    });

    holder.mContainer.setSelected(mChoice.isPositionCheck(position - 1));
  }

  @Override
  public String getSectionText(int position) {
    if (position == 0) {
      return "";
    }
    if (mDatas != null && position - 1 < mDatas.size()) {
      String title = mDatas.get(position - 1).getTitle();
      return !TextUtils.isEmpty(title) ? (Pinyin.toPinyin(title.charAt(0))).toUpperCase()
          .substring(0, 1) : "";
    }
    return "";
  }

  /**
   * 更新高亮歌曲
   */
  public void updatePlayingSong() {
    final Song currentSong = MusicServiceRemote.getCurrentSong();
    if (currentSong.getId() == -1 || currentSong.getId() == mLastPlaySong.getId()) {
      return;
    }

    if (mDatas != null && mDatas.indexOf(currentSong) >= 0) {
      // 找到新的高亮歌曲
      final int index = mDatas.indexOf(currentSong) + 1;
      final int lastIndex = mDatas.indexOf(mLastPlaySong) + 1;

      SongViewHolder newHolder = null;
      if (mRecyclerView.findViewHolderForAdapterPosition(index) instanceof SongViewHolder) {
        newHolder = (SongViewHolder) mRecyclerView.findViewHolderForAdapterPosition(index);
      }
      SongViewHolder oldHolder = null;
      if (mRecyclerView.findViewHolderForAdapterPosition(lastIndex) instanceof SongViewHolder) {
        oldHolder = (SongViewHolder) mRecyclerView.findViewHolderForAdapterPosition(lastIndex);
      }

      if (newHolder != null) {
        newHolder.mName.setTextColor(getHighLightTextColor());
        newHolder.mIndicator.setVisibility(View.VISIBLE);
      }

      if (oldHolder != null) {
        oldHolder.mName.setTextColor(getTextColorPrimary());
        oldHolder.mIndicator.setVisibility(View.GONE);
      }
      mLastPlaySong = currentSong;
    }
  }

  static class SongViewHolder extends BaseViewHolder {

    @BindView(R.id.song_title)
    TextView mName;
    @BindView(R.id.song_other)
    TextView mOther;
    @BindView(R.id.song_head_image)
    SimpleDraweeView mImage;
    @BindView(R.id.song_button)
    ImageButton mButton;
    @BindView(R.id.item_root)
    View mContainer;
    @BindView(R.id.indicator)
    View mIndicator;


    SongViewHolder(View itemView) {
      super(itemView);
    }
  }

  static class HeaderHolder extends BaseViewHolder {

    View mRoot;
    @BindView(R.id.divider)
    View mDivider;
    @BindView(R.id.play_shuffle_button)
    ImageView mShuffleIv;

    HeaderHolder(View itemView) {
      super(itemView);
      mRoot = itemView;
    }
  }
}
