package tanzent.cassette.ui.dialog;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.SwitchCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import tanzent.cassette.R;
import tanzent.cassette.helper.SleepTimer;
import tanzent.cassette.misc.handler.MsgHandler;
import tanzent.cassette.misc.handler.OnHandleMessage;
import tanzent.cassette.theme.Theme;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.ui.widget.CircleSeekBar;
import tanzent.cassette.util.ColorUtil;
import tanzent.cassette.util.DensityUtil;
import tanzent.cassette.util.LogUtil;
import tanzent.cassette.util.SPUtil;
import tanzent.cassette.util.ToastUtil;

/**
 * Created by taeja on 16-1-15.
 */

/**
 * 定时关闭界面
 */
public class TimerDialog extends BaseDialogActivity {
    //提示信息
    @BindView(R.id.timer_info_container)
    View mInfoContainer;
    @BindView(R.id.timer_content_container)
    View mContentContainer;
    //分钟
    @BindView(R.id.minute)
    TextView mMinute;
    //秒
    @BindView(R.id.second)
    TextView mSecond;
    //设置或取消默认
    SwitchCompat mSwitch;
    //圆形seekbar
    @BindView(R.id.close_seekbar)
    CircleSeekBar mSeekbar;
    //开始或取消计时
    @BindView(R.id.close_toggle)
    TextView mToggle;
    @BindView(R.id.close_stop)
    TextView mCancel;


    //定时时间 单位秒
    private int mTime;
    //设置的定时时间 用于保存默认设置
    private static int mSaveTime = -1;
    //每一秒中更新数据
    private Timer mUpdateTimer;
    //更新seekbar与剩余时间
    private MsgHandler mHandler;


    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_timer);
        ButterKnife.bind(this);
        mHandler = new MsgHandler(this);

        //居中显示
        Window w = getWindow();
        final WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        final DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        w.setAttributes(lp);
        w.setGravity(Gravity.CENTER);


        //如果正在计时，设置seekbar的进度
        if (SleepTimer.isTicking()) {
            mTime = (int) (SleepTimer.getMillisUntilFinish() / 1000);
            if (mTime > 0) {
                mSeekbar.setProgress(mTime);
                mSeekbar.setStart(true);
            }
        }

        mSeekbar.setOnSeekBarChangeListener(new CircleSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircleSeekBar seekBar, long progress, boolean fromUser) {
                if (progress > 0) {
                    //记录倒计时时间和更新界面
                    int minute = (int) (progress / 60);
                    mMinute.setText(minute < 10 ? "0" + minute : "" + minute);
                    mSecond.setText("00");
                    //取整数分钟
                    mTime = minute * 60;
                    mSaveTime = minute * 60;
                    LogUtil.e("TimerDialog", "设置时间: " + mSaveTime + "s");
                }
            }

            @Override
            public void onStartTrackingTouch(CircleSeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(CircleSeekBar seekBar) {
            }
        });

        //初始化switch
        mSwitch = new SwitchCompat(new ContextThemeWrapper(this, ThemeStore.isDay() ? Theme.getTheme() : R.style.TimerDialogNightTheme));
        ((LinearLayout) findViewById(R.id.popup_timer_container)).addView(mSwitch);

        //读取保存的配置
        boolean hasDefault = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DEFAULT, false);
        final int time = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DURATION, -1);

        //默认选项
        if (hasDefault && time > 0) {
            //如果有默认设置并且没有开始计时，直接开始计时
            //如果有默认设置但已经开始计时，打开该popupwindow,并更改switch外观
            if (!SleepTimer.isTicking()) {
                mTime = time;
                toggle();
            }
        }
        mSwitch.setChecked(hasDefault);
        mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (mSaveTime > 0) {
                    ToastUtil.show(TimerDialog.this, R.string.set_success);
                    SPUtil.putValue(TimerDialog.this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DEFAULT, true);
                    SPUtil.putValue(TimerDialog.this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DURATION, mSaveTime);
                } else {
                    ToastUtil.show(TimerDialog.this, R.string.plz_set_correct_time);
                    mSwitch.setChecked(false);
                }
            } else {
                ToastUtil.show(TimerDialog.this, R.string.cancel_success);
                SPUtil.putValue(TimerDialog.this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DEFAULT, false);
                SPUtil.putValue(TimerDialog.this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DURATION, -1);
                mSaveTime = -1;
            }
        });

        mToggle.setText(SleepTimer.isTicking() ? R.string.cancel_timer : R.string.start_timer);

        //分钟 秒 背景框
        ButterKnife.apply(new View[]{findViewById(R.id.timer_minute_container), findViewById(R.id.timer_second_container)},
                (view, index) -> {
                    final Drawable drawable = Theme.getShape(
                            GradientDrawable.RECTANGLE,
                            Color.TRANSPARENT,
                            DensityUtil.dip2px(mContext, 1),
                            DensityUtil.dip2px(mContext, 1),
                            ColorUtil.getColor(R.color.gray_404040),
                            0, 0, 1);
                    view.setBackground(drawable);
                });

    }

    /**
     * 根据是否已经开始计时来取消或开始计时
     */
    private void toggle() {
        if (mTime <= 0 && !SleepTimer.isTicking()) {
            ToastUtil.show(TimerDialog.this, R.string.plz_set_correct_time);
            return;
        }

        //如果开始计时，保存设置的时间
//        if(mIsTiming){
//            mSaveTime = mTime / 60;
//        }
        SleepTimer.toggleTimer(mTime * 1000);
        finish();
    }

    @OnClick({R.id.timer_info, R.id.timer_info_close, R.id.close_stop, R.id.close_toggle})
    public void OnClick(View view) {
        switch (view.getId()) {
            case R.id.timer_info:
                mContentContainer.setVisibility(View.INVISIBLE);
                mInfoContainer.setVisibility(View.VISIBLE);
                break;
            case R.id.timer_info_close:
                mInfoContainer.setVisibility(View.INVISIBLE);
                mContentContainer.setVisibility(View.VISIBLE);
                break;
            case R.id.close_stop:
                finish();
                break;
            case R.id.close_toggle:
                toggle();
                break;
        }
    }

    @OnHandleMessage
    public void handlerInternal(Message msg) {
        if (msg != null) {
            if (msg.getData() != null) {
                mMinute.setText(msg.getData().getString("Minute"));
                mSecond.setText(msg.getData().getString("Second"));
            }
            mSeekbar.setProgress(msg.arg1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (SleepTimer.isTicking()) {
            mUpdateTimer = new Timer();
            mUpdateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    int min, sec, remain;
                    remain = (int) SleepTimer.getMillisUntilFinish() / 1000;
                    min = remain / 60;
                    sec = remain % 60;
                    Message msg = new Message();
                    msg.arg1 = remain;
                    Bundle data = new Bundle();
                    data.putString("Minute", min < 10 ? "0" + min : "" + min);
                    data.putString("Second", sec < 10 ? "0" + sec : "" + sec);
                    LogUtil.e("TimerDialog", "Minute: " + min + " Second: " + sec);
                    msg.setData(data);
                    mHandler.sendMessage(msg);
                }
            }, 0, 1000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUpdateTimer != null) {
            mUpdateTimer.cancel();
            mUpdateTimer = null;
        }
        mHandler.remove();
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(android.R.anim.fade_in, 0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

}
