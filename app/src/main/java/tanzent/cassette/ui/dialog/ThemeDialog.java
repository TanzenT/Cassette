package tanzent.cassette.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import tanzent.cassette.R;
import tanzent.cassette.bean.mp3.ColorChoose;
import tanzent.cassette.theme.Theme;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.ui.activity.SettingActivity;
import tanzent.cassette.util.ColorUtil;

/**
 * @ClassName ThemeDialog
 * @Description 主题颜色选择
 * @Author Xiaoborui
 * @Date 2016/8/26 11:14
 */
public class ThemeDialog extends BaseDialogActivity {
    @BindView(R.id.color_container)
    LinearLayout mColorContainer;
    private ArrayList<ColorChoose> mColorInfoList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_color_choose);
        ButterKnife.bind(this);
        final int[] mColors = new int[]{R.color.md_blue_primary, R.color.md_red_primary, R.color.md_brown_primary, R.color.md_navy_primary,
                R.color.md_green_primary, R.color.md_yellow_primary, R.color.md_purple_primary, R.color.md_indigo_primary, R.color.md_plum_primary,
                R.color.md_pink_primary, R.color.md_white_primary_dark};
        final int[] mThemeColors = new int[]{ThemeStore.THEME_BLUE, ThemeStore.THEME_RED, ThemeStore.THEME_BROWN, ThemeStore.THEME_NAVY,
                ThemeStore.THEME_GREEN, ThemeStore.THEME_YELLOW, ThemeStore.THEME_PURPLE, ThemeStore.THEME_INDIGO, ThemeStore.THEME_PLUM,
                ThemeStore.THEME_PINK, ThemeStore.THEME_WHITE};
        final String[] mColorTexts = new String[]{getString(R.string.color_default), getString(R.string.color_red), getString(R.string.color_gray), getString(R.string.color_dark_green), getString(R.string.color_green), getString(R.string.color_yellow),
                getString(R.string.color_purple), getString(R.string.color_blueberry), getString(R.string.color_hotpink), getString(R.string.color_pink), getString(R.string.color_white)};


        for (int i = 0; i < mColors.length; i++) {
            addColor(mColors[i], mColorTexts[i], mThemeColors[i]);
        }
    }

    /**
     * 添加颜色
     */
    private void addColor(@ColorRes int mdColor, String colorText, int themeColor) {
        View colorItem = LayoutInflater.from(this).inflate(R.layout.item_color_choose, null);
        ImageView src = colorItem.findViewById(R.id.color_choose_item_src);
        GradientDrawable drawable = (GradientDrawable) src.getDrawable();
        drawable.setColor(ColorUtil.getColor(mdColor));

        ImageView check = colorItem.findViewById(R.id.color_choose_item_check);
        check.setVisibility(isColorChoose(themeColor) ? View.VISIBLE : View.GONE);

        TextView colorTextView = colorItem.findViewById(R.id.color_choose_item_text);
        colorTextView.setText(colorText);
        colorTextView.setTextColor(ThemeStore.getTextColorPrimary());

        colorItem.setOnClickListener(new ColorListener(themeColor));
        mColorContainer.addView(colorItem);
        mColorInfoList.add(new ColorChoose(themeColor, colorText, check));
    }

    /**
     * 判断是否是选中的颜色
     *
     * @param color
     * @return
     */
    private boolean isColorChoose(int color) {
        return color == ThemeStore.THEME_COLOR;
    }


    class ColorListener implements View.OnClickListener {
        private int mThemeColor;

        ColorListener(int themeColor) {
            mThemeColor = themeColor;
        }

        @Override
        public void onClick(View v) {

            if (!ThemeStore.isDay()) {
                Theme.getBaseDialog(mContext)
                        .content(R.string.whether_change_theme)
                        .buttonRippleColor(ThemeStore.getRippleColor())
                        .positiveText(R.string.confirm)
                        .negativeText(R.string.cancel)
                        .onPositive((dialog, which) -> changeThemeColor(true))
                        .onNegative((dialog, which) -> {

                        }).
                        show();
            } else {
                changeThemeColor(false);
            }
        }

        private void changeThemeColor(boolean isFromNight) {
            Intent intent = new Intent();
            intent.putExtra("needRecreate", true);
            intent.putExtra("fromColorChoose", isFromNight);
            setResult(Activity.RESULT_OK, intent);
            ThemeStore.THEME_MODE = ThemeStore.DAY;
            ThemeStore.THEME_COLOR = mThemeColor;
            ThemeStore.MATERIAL_COLOR_PRIMARY = ThemeStore.getMaterialPrimaryColorRes();
            ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = ThemeStore.getMaterialPrimaryDarkColorRes();
            ThemeStore.saveThemeColor(ThemeStore.THEME_COLOR);
            ThemeStore.saveThemeMode(ThemeStore.THEME_MODE);
            finish();
        }
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
