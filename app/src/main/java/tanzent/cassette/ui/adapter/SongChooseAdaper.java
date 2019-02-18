package tanzent.cassette.ui.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;

import butterknife.BindView;
import tanzent.cassette.R;
import tanzent.cassette.bean.mp3.Song;
import tanzent.cassette.misc.interfaces.OnSongChooseListener;
import tanzent.cassette.request.LibraryUriRequest;
import tanzent.cassette.request.RequestConfig;
import tanzent.cassette.ui.adapter.holder.BaseViewHolder;

import static tanzent.cassette.request.ImageUriRequest.SMALL_IMAGE_SIZE;
import static tanzent.cassette.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/21 10:02
 */

public class SongChooseAdaper extends BaseAdapter<Song, SongChooseAdaper.SongChooseHolder> {
    private OnSongChooseListener mCheckListener;
    private ArrayList<Integer> mCheckSongIdList = new ArrayList<>();

    public SongChooseAdaper(Context context, int layoutID, OnSongChooseListener l) {
        super(context, layoutID);
        mCheckListener = l;
    }

    public ArrayList<Integer> getCheckedSong() {
        return mCheckSongIdList;
    }

    @Override
    protected void convert(final SongChooseHolder holder, Song song, int position) {
        //歌曲名
        holder.mSong.setText(song.getTitle());
        //艺术家
        holder.mArtist.setText(song.getArtist());
        //封面
        holder.mImage.setImageURI(Uri.EMPTY);

        new LibraryUriRequest(holder.mImage,
                getSearchRequestWithAlbumType(song),
                new RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()).load();
        //选中歌曲
        holder.mRoot.setOnClickListener(v -> {
            holder.mCheck.setChecked(!holder.mCheck.isChecked());
            mCheckListener.OnSongChoose(mCheckSongIdList != null && mCheckSongIdList.size() > 0);
        });

        final int audioId = song.getId();
        holder.mCheck.setOnCheckedChangeListener(null);
        holder.mCheck.setChecked(mCheckSongIdList != null && mCheckSongIdList.contains(audioId));
        holder.mCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !mCheckSongIdList.contains(audioId)) {
                mCheckSongIdList.add(audioId);
            } else if (!isChecked) {
                mCheckSongIdList.remove(Integer.valueOf(audioId));
            }
            mCheckListener.OnSongChoose(mCheckSongIdList != null && mCheckSongIdList.size() > 0);
        });
    }

    static class SongChooseHolder extends BaseViewHolder {
        @BindView(R.id.checkbox)
        AppCompatCheckBox mCheck;
        @BindView(R.id.item_img)
        SimpleDraweeView mImage;
        @BindView(R.id.item_song)
        TextView mSong;
        @BindView(R.id.item_album)
        TextView mArtist;
        @BindView(R.id.item_root)
        RelativeLayout mRoot;

        SongChooseHolder(View itemView) {
            super(itemView);
        }
    }
}
