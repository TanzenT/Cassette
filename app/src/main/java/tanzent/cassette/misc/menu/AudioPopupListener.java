package tanzent.cassette.misc.menu;

import android.content.ContextWrapper;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import tanzent.cassette.App;
import tanzent.cassette.Global;
import tanzent.cassette.R;
import tanzent.cassette.bean.mp3.PlayListSong;
import tanzent.cassette.bean.mp3.Song;
import tanzent.cassette.helper.MusicServiceRemote;
import tanzent.cassette.service.Command;
import tanzent.cassette.service.MusicService;
import tanzent.cassette.ui.Tag;
import tanzent.cassette.ui.activity.EQActivity;
import tanzent.cassette.ui.activity.PlayerActivity;
import tanzent.cassette.ui.dialog.AddtoPlayListDialog;
import tanzent.cassette.ui.dialog.FileChooserDialog;
import tanzent.cassette.ui.dialog.TimerDialog;
import tanzent.cassette.ui.fragment.LyricFragment;
import tanzent.cassette.util.Constants;
import tanzent.cassette.util.MediaStoreUtil;
import tanzent.cassette.util.PlayListUtil;
import tanzent.cassette.util.SPUtil;
import tanzent.cassette.util.ToastUtil;
import tanzent.cassette.util.Util;

import static com.afollestad.materialdialogs.DialogAction.POSITIVE;
import static tanzent.cassette.helper.MusicServiceRemote.getCurrentSong;
import static tanzent.cassette.helper.MusicServiceRemote.getMediaPlayer;
import static tanzent.cassette.theme.Theme.getBaseDialog;
import static tanzent.cassette.util.Util.sendLocalBroadcast;

/**
 * @ClassName AudioPopupListener
 * @Description
 * @Author Xiaoborui
 * @Date 2016/8/29 15:33
 */
public class AudioPopupListener<ActivityCallback extends AppCompatActivity & FileChooserDialog.FileCallback> extends ContextWrapper implements PopupMenu.OnMenuItemClickListener {
    private ActivityCallback mActivity;
    private Song mInfo;
    private Tag mTag;

    public AudioPopupListener(ActivityCallback activity, Song song) {
        super(activity);
        mActivity = activity;
        mInfo = song;
        mTag = new Tag(activity, song);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_lyric:
                final boolean alreadyIgnore = SPUtil.getValue(mActivity, SPUtil.LYRIC_KEY.NAME, mInfo.getId() + "", SPUtil.LYRIC_KEY.LYRIC_DEFAULT) == SPUtil.LYRIC_KEY.LYRIC_IGNORE;
                final LyricFragment lyricFragment = ((PlayerActivity) mActivity).getLyricFragment();
                getBaseDialog(mActivity)
                        .items(getString(R.string.netease),
                                getString(R.string.kugou),
                                getString(R.string.local),
                                getString(R.string.embedded_lyric),
                                getString(R.string.select_lrc),
                                getString(!alreadyIgnore ? R.string.ignore_lrc : R.string.cancel_ignore_lrc),
                                getString(R.string.change_offset))
                        .itemsColorAttr(R.attr.text_color_primary)
                        .backgroundColorAttr(R.attr.background_color_3)
                        .itemsCallback((dialog, itemView, position, text) -> {
                            switch (position) {
                                case 0: //网易
                                case 1://酷狗
                                case 2://本地
                                case 3://内嵌
                                    SPUtil.putValue(mActivity, SPUtil.LYRIC_KEY.NAME, mInfo.getId() + "", position + 2);
                                    lyricFragment.updateLrc(mInfo, true);
                                    sendLocalBroadcast(new Intent(MusicService.ACTION_CMD).putExtra("Control", Command.CHANGE_LYRIC));
                                    break;
                                case 4: //手动选择歌词
                                    new FileChooserDialog.Builder(mActivity)
                                            .extensionsFilter(".lrc")
                                            .show();
                                    break;
                                case 5: //忽略或者取消忽略
                                    getBaseDialog(mActivity)
                                            .title(!alreadyIgnore ? R.string.confirm_ignore_lrc : R.string.confirm_cancel_ignore_lrc)
                                            .negativeText(R.string.cancel)
                                            .positiveText(R.string.confirm)
                                            .onPositive((dialog1, which) -> {
                                                if (!alreadyIgnore) {//忽略
                                                    if (mInfo != null) {
                                                        SPUtil.putValue(mActivity, SPUtil.LYRIC_KEY.NAME, mInfo.getId() + "", SPUtil.LYRIC_KEY.LYRIC_IGNORE);
                                                        lyricFragment.updateLrc(mInfo);
                                                    }
                                                } else {//取消忽略
                                                    SPUtil.putValue(mActivity, SPUtil.LYRIC_KEY.NAME, mInfo.getId() + "", SPUtil.LYRIC_KEY.LYRIC_DEFAULT);
                                                    lyricFragment.updateLrc(mInfo);
                                                }
                                                sendLocalBroadcast(new Intent(MusicService.ACTION_CMD).putExtra("Control", Command.CHANGE_LYRIC));
                                            })
                                            .show();
                                    break;
                                case 6://歌词时间轴调整
                                    ((PlayerActivity) mActivity).showLyricOffsetView();
                                    break;
                            }

                        })
                        .show();
                break;
            case R.id.menu_edit:
                mTag.edit();
                break;
            case R.id.menu_detail:
                mTag.detail();
                break;
            case R.id.menu_timer:
                mActivity.startActivity(new Intent(mActivity, TimerDialog.class));
                break;
            case R.id.menu_eq:
                Intent audioEffectIntent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getMediaPlayer().getAudioSessionId());
                if (Util.isIntentAvailable(mActivity, audioEffectIntent)) {
                    mActivity.startActivityForResult(audioEffectIntent, 0);
                } else {
                    mActivity.startActivity(new Intent(mActivity, EQActivity.class));
                }
                break;
            case R.id.menu_collect:
                PlayListSong info = new PlayListSong(mInfo.getId(), Global.MyLoveID, Constants.MYLOVE);
                ToastUtil.show(mActivity,
                        PlayListUtil.addSong(info) > 0 ? getString(R.string.add_song_playlist_success, 1, Constants.MYLOVE) : getString(R.string.add_song_playlist_error));
                break;
            case R.id.menu_add_to_playlist:
                Intent intentAdd = new Intent(this, AddtoPlayListDialog.class);
                Bundle ardAdd = new Bundle();
                ardAdd.putSerializable("list", new ArrayList<>(Collections.singletonList(mInfo.getId())));
                intentAdd.putExtras(ardAdd);
                startActivity(intentAdd);
                break;
            case R.id.menu_delete:
                getBaseDialog(mActivity)
                        .content(R.string.confirm_delete_from_library)
                        .positiveText(R.string.confirm)
                        .negativeText(R.string.cancel)
                        .checkBoxPromptRes(R.string.delete_source, SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE, false), null)
                        .onAny((dialog, which) -> {
                            if (which == POSITIVE) {
                                if (MediaStoreUtil.delete(mInfo.getId(), Constants.SONG, dialog.isPromptCheckBoxChecked()) > 0) {
                                    ToastUtil.show(mActivity, getString(R.string.delete_success));
                                    //移除的是正在播放的歌曲
                                    if (mInfo.getId() == getCurrentSong().getId()) {
                                        Intent intent = new Intent(MusicService.ACTION_CMD);
                                        intent.putExtra("Control", Command.NEXT);
                                        Util.sendLocalBroadcast(intent);
                                    }
                                } else {
                                    ToastUtil.show(mActivity, getString(R.string.delete_error));
                                }
                            }
                        })
                        .show();
                break;
//            case R.id.menu_vol:
//                AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
//                if(audioManager != null){
//                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
//                }
            case R.id.menu_speed:
                final List<String> speeds = Arrays.asList("0.5", "0.75", "1.0", "1.25", "1.5");
                final String originalSpeed = SPUtil.getValue(mActivity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SPEED, "1.0");
                getBaseDialog(mActivity)
                        .title(R.string.speed)
                        .items(speeds)
                        .itemsCallbackSingleChoice(speeds.indexOf(originalSpeed), (dialog, itemView, which, text) -> {
                            MusicServiceRemote.setSpeed(Float.parseFloat(text.toString()));
                            SPUtil.putValue(mActivity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SPEED, text.toString());
                            return true;
                        }).show();
                break;
        }
        return true;
    }

}
