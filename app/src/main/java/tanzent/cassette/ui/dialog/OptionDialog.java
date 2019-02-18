package tanzent.cassette.ui.dialog;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import tanzent.cassette.App;
import tanzent.cassette.R;
import tanzent.cassette.bean.mp3.Song;
import tanzent.cassette.request.LibraryUriRequest;
import tanzent.cassette.request.RequestConfig;
import tanzent.cassette.theme.Theme;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.util.ColorUtil;
import tanzent.cassette.util.Constants;
import tanzent.cassette.util.ImageUriUtil;
import tanzent.cassette.util.MediaStoreUtil;
import tanzent.cassette.util.PlayListUtil;
import tanzent.cassette.util.SPUtil;
import tanzent.cassette.util.ToastUtil;
import tanzent.cassette.util.Util;

import static com.afollestad.materialdialogs.DialogAction.POSITIVE;
import static tanzent.cassette.request.ImageUriRequest.SMALL_IMAGE_SIZE;

/**
 * Created by Remix on 2015/12/6.
 */

/**
 * 歌曲的选项对话框
 */
@Deprecated
public class OptionDialog extends BaseDialogActivity {
    //添加 设置铃声 分享 删除按钮
    @BindView(R.id.popup_add)
    View mAdd;
    @BindView(R.id.popup_ring)
    View mRing;
    @BindView(R.id.popup_share)
    View mShare;
    @BindView(R.id.popup_delete)
    View mDelete;

    //标题
    @BindView(R.id.popup_title)
    TextView mTitle;
    //专辑封面
    @BindView(R.id.popup_image)
    SimpleDraweeView mDraweeView;

    //当前正在播放的歌曲
    private Song mSong = null;
    //是否是删除播放列表中歌曲
    private boolean mIsDeletePlayList = false;
    //播放列表名字
    private String mPlayListName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_option);
        ButterKnife.bind(this);

        mSong = getIntent().getExtras().getParcelable("Song");
        if (mSong == null)
            return;
        if (mIsDeletePlayList = getIntent().getExtras().getBoolean("IsDeletePlayList", false)) {
            mPlayListName = getIntent().getExtras().getString("PlayListName");
        }

        //设置歌曲名与封面
        mTitle.setText(String.format("%s-%s", mSong.getTitle(), mSong.getArtist()));
        new LibraryUriRequest(mDraweeView, ImageUriUtil.getSearchRequestWithAlbumType(mSong), new RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()).load();
        //置于底部
        Window w = getWindow();
//        w.setWindowAnimations(R.style.AnimBottom);
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = (metrics.widthPixels);
        w.setAttributes(lp);
        w.setGravity(Gravity.BOTTOM);

        //为按钮着色
        final int tintColor = ThemeStore.isDay() ?
                ColorUtil.getColor(R.color.day_textcolor_primary) :
                ColorUtil.getColor(R.color.white_f4f4f5);
        ((ImageView) findViewById(R.id.popup_add_img)).setImageDrawable(Theme.TintDrawable(getResources().getDrawable(R.drawable.pop_btn_add2list), tintColor));
        ((ImageView) findViewById(R.id.popup_ring_img)).setImageDrawable(Theme.TintDrawable(getResources().getDrawable(R.drawable.pop_btn_ring), tintColor));
        ((ImageView) findViewById(R.id.popup_share_img)).setImageDrawable(Theme.TintDrawable(getResources().getDrawable(R.drawable.pop_btn_share), tintColor));
        ((ImageView) findViewById(R.id.popup_delete_img)).setImageDrawable(Theme.TintDrawable(getResources().getDrawable(R.drawable.pop_btn_delete), tintColor));

        ButterKnife.apply(new TextView[]{findViewById(R.id.popup_add_text), findViewById(R.id.popup_ring_text),
                        findViewById(R.id.popup_share_text), findViewById(R.id.popup_delete_text)},
                (textView, index) -> textView.setTextColor(tintColor));
    }

    @OnClick({R.id.popup_add, R.id.popup_share, R.id.popup_delete, R.id.popup_ring})
    public void onClick(View v) {
        switch (v.getId()) {
            //添加到播放列表
            case R.id.popup_add:
                Intent intentAdd = new Intent(OptionDialog.this, AddtoPlayListDialog.class);
                Bundle ardAdd = new Bundle();
                ardAdd.putInt("Id", mSong.getId());
                intentAdd.putExtras(ardAdd);
                startActivity(intentAdd);
                finish();
                break;
            //设置铃声
            case R.id.popup_ring:
                MediaStoreUtil.setRing(this, mSong.getId());
                finish();
                break;
            //分享
            case R.id.popup_share:
                startActivity(
                        Intent.createChooser(Util.createShareSongFileIntent(mSong, mContext), null));
                finish();
                break;
            //删除
            case R.id.popup_delete:
                try {
                    String title = getString(R.string.confirm_delete_from_playlist_or_library, mIsDeletePlayList ? mPlayListName : getString(R.string.song_list));
                    Theme.getBaseDialog(mContext)
                            .content(title)
                            .positiveText(R.string.confirm)
                            .negativeText(R.string.cancel)
                            .checkBoxPromptRes(R.string.delete_source, SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE, false), null)
                            .onAny((dialog, which) -> {
                                if (which == POSITIVE) {
                                    boolean deleteSuccess = !mIsDeletePlayList ?
                                            MediaStoreUtil.delete(mSong.getId(), Constants.SONG, dialog.isPromptCheckBoxChecked()) > 0 :
                                            PlayListUtil.deleteSong(mSong.getId(), mPlayListName);

                                    ToastUtil.show(mContext, deleteSuccess ? R.string.delete_success : R.string.delete_error);
                                    finish();
                                }
                            })
                            .show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.slide_bottom_in, 0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_bottom_out);
    }


}
