package tanzent.cassette.ui.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import tanzent.cassette.R;
import tanzent.cassette.bean.misc.Feedback;
import tanzent.cassette.theme.Theme;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.util.ColorUtil;
import tanzent.cassette.util.DensityUtil;
import tanzent.cassette.util.ToastUtil;
import tanzent.cassette.util.Util;

import static tanzent.cassette.App.IS_GOOGLEPLAY;

/**
 * Created by taeja on 16-3-7.
 */

/**
 * 反馈界面
 * 将用户的反馈通过邮箱发送
 */
public class FeedBackActivity extends ToolbarActivity {
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
    @BindView(R.id.feedback_content)
    EditText mContent;
    @BindView(R.id.feedback_contact)
    EditText mContact;
    @BindView(R.id.feedback_submit)
    Button mSubmit;

    Feedback mFeedBack = new Feedback();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        ButterKnife.bind(this);
        setUpToolbar(mToolBar, getString(R.string.back));

        mSubmit.setBackground(Theme.getCorner(1.0f, DensityUtil.dip2px(this, 2), 0, ThemeStore.getAccentColor()));
        mContent.setBackground(Theme.getCorner(1.0f, DensityUtil.dip2px(this, 2), 0, ColorUtil.getColor(R.color.gray_e2e2e2)));
        Theme.setTint(mContact, ThemeStore.getMaterialPrimaryColor(), false);
    }

    @OnClick(R.id.feedback_submit)
    public void onClick(View v) {
        try {
            if (TextUtils.isEmpty(mContent.getText())) {
                ToastUtil.show(this, getString(R.string.input_feedback_content));
                return;
            }
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
            mFeedBack = new Feedback(mContent.getText().toString(),
                    mContact.getText().toString(),
                    pi.versionName,
                    pi.versionCode + "",
                    Build.DISPLAY,
                    Build.CPU_ABI + "," + Build.CPU_ABI2,
                    Build.MANUFACTURER,
                    Build.MODEL,
                    Build.VERSION.RELEASE,
                    Build.VERSION.SDK_INT + ""
            );
            commitByEmail();
        } catch (PackageManager.NameNotFoundException e) {
            ToastUtil.show(this, R.string.send_error);
        }
    }

    private void commitByEmail() {
        Intent data = new Intent(Intent.ACTION_SENDTO);
        data.setData(Uri.parse(!IS_GOOGLEPLAY ? "mailto:zeus8502@gmail.com" : "mailto:zeus8502@gmail.com"));
        data.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback));
        data.putExtra(Intent.EXTRA_TEXT, mContent.getText().toString() + "\n\n\n" + mFeedBack);
        if (Util.isIntentAvailable(this, data)) {
            startActivity(data);
        } else {
            ToastUtil.show(this, R.string.send_error);
        }

    }
}
