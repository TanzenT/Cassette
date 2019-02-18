package tanzent.cassette.ui.dialog;

import android.os.Bundle;
import android.view.View;

import tanzent.cassette.R;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.ui.activity.base.BaseMusicActivity;

/**
 * Created by Remix on 2016/3/16.
 */


public abstract class BaseDialogActivity extends BaseMusicActivity {
    protected <T extends View> T findView(int id) {
        return (T) findViewById(id);
    }

    @Override
    protected void setUpTheme() {
        setTheme(ThemeStore.isDay() ? R.style.Dialog_DayTheme : R.style.Dialog_NightTheme);
    }

    @Override
    protected void setStatusBar() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("");
    }

}
