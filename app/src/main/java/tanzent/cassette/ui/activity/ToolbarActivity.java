package tanzent.cassette.ui.activity;

import android.annotation.SuppressLint;
import android.support.v7.widget.Toolbar;

import tanzent.cassette.R;
import tanzent.cassette.theme.Theme;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.ui.activity.base.BaseMusicActivity;
import tanzent.cassette.util.ColorUtil;


/**
 * Created by taeja on 16-3-15.
 */
@SuppressLint("Registered")
public class ToolbarActivity extends BaseMusicActivity {
    protected void setUpToolbar(Toolbar toolbar, String title) {
        toolbar.setTitle(title);

        setSupportActionBar(toolbar);
        //主题颜色
        int themeColor = ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.black : R.color.white);
        toolbar.setNavigationIcon(Theme.TintDrawable(R.drawable.common_btn_back, themeColor));
        toolbar.setTitleTextColor(themeColor);

        toolbar.setNavigationOnClickListener(v -> onClickNavigation());
//        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                switch (item.getItemId()) {
//                    case R.id.toolbar_search:
//                        startActivity(new Intent(mContext, SearchActivity.class));
//                        break;
//                    case R.id.toolbar_timer:
//                        startActivity(new Intent(mContext, TimerDialog.class));
//                        break;
//                }
//                return true;
//            }
//        });
    }

    protected void onClickNavigation() {
        finish();
    }

}
