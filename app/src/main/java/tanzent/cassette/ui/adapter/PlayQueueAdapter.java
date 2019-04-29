package tanzent.cassette.ui.adapter;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import java.util.Collections;
import tanzent.cassette.R;
import tanzent.cassette.bean.mp3.Song;
import tanzent.cassette.db.room.DatabaseRepository;
import tanzent.cassette.helper.MusicServiceRemote;
import tanzent.cassette.request.network.RxUtil;
import tanzent.cassette.service.Command;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.ui.adapter.holder.BaseViewHolder;
import tanzent.cassette.util.Util;

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 正在播放列表的适配器
 */
public class PlayQueueAdapter extends BaseAdapter<Song, PlayQueueAdapter.PlayQueueHolder> {

  private int mAccentColor;
  private int mTextColor;

  public PlayQueueAdapter(Context context, int layoutId) {
    super(context, layoutId);
    mAccentColor = ThemeStore.getAccentColor();
    mTextColor = ThemeStore.getTextColorPrimary();
  }

  @Override
  protected void convert(final PlayQueueHolder holder, Song song, int position) {
    if (song == null) {
      //歌曲已经失效
      holder.mSong.setText(mContext.getString(R.string.song_lose_effect));
      holder.mArtist.setVisibility(View.GONE);
      return;
    }
    //设置歌曲与艺术家
    holder.mSong.setText(song.getShowName());
    holder.mArtist.setText(song.getArtist());
    holder.mArtist.setVisibility(View.VISIBLE);
    //高亮
    if (MusicServiceRemote.getCurrentSong().getId() == song.getId()) {
      holder.mSong.setTextColor(mAccentColor);
    } else {
//                holder.mSong.setTextColor(Color.parseColor(ThemeStore.isDay() ? "#323335" : "#ffffff"));
      holder.mSong.setTextColor(mTextColor);
    }
    //删除按钮
    holder.mDelete.setOnClickListener(v -> {
      DatabaseRepository.getInstance()
          .deleteFromPlayQueue(Collections.singletonList(song.getId()))
          .compose(RxUtil.applySingleScheduler())
          .subscribe(num -> {
            //删除的是当前播放的歌曲
            if (num > 0 && MusicServiceRemote.getCurrentSong().getId() == song.getId()) {
              Util.sendCMDLocalBroadcast(Command.NEXT);
            }
          });
    });
    if (mOnItemClickListener != null) {
      holder.mContainer.setOnClickListener(
          v -> mOnItemClickListener.onItemClick(v, holder.getAdapterPosition()));
    }

  }

  static class PlayQueueHolder extends BaseViewHolder {

    @BindView(R.id.playlist_item_name)
    TextView mSong;
    @BindView(R.id.playlist_item_artist)
    TextView mArtist;
    @BindView(R.id.playqueue_delete)
    ImageView mDelete;
    @BindView(R.id.item_root)
    RelativeLayout mContainer;

    public PlayQueueHolder(View v) {
      super(v);
    }
  }
}
