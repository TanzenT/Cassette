package tanzent.cassette.ui.adapter;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import tanzent.cassette.R;
import tanzent.cassette.db.room.model.PlayList;
import tanzent.cassette.ui.adapter.holder.BaseViewHolder;

/**
 * Created by taeja on 16-2-1.
 */

/**
 * 将歌曲添加到播放列表的适配器
 */
public class AddtoPlayListAdapter extends BaseAdapter<PlayList, AddtoPlayListAdapter.PlayListAddToHolder> {

  public AddtoPlayListAdapter(Context context, int layoutId) {
    super(context, layoutId);
  }


  @Override
  protected void convert(PlayListAddToHolder holder, PlayList playList, int position) {
    holder.mText.setText(playList.getName());
    holder.mText.setTag(playList.getId());
    holder.mContainer.setOnClickListener(v -> {
      if (mOnItemClickListener != null) {
        mOnItemClickListener.onItemClick(v, position);
      }
    });
  }


  static class PlayListAddToHolder extends BaseViewHolder {

    @BindView(R.id.playlist_addto_text)
    TextView mText;
    @BindView(R.id.item_root)
    RelativeLayout mContainer;

    PlayListAddToHolder(View itemView) {
      super(itemView);
    }
  }
}
