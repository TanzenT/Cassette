package tanzent.cassette.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import butterknife.BindView;
import java.util.Arrays;
import java.util.List;
import tanzent.cassette.R;
import tanzent.cassette.theme.GradientDrawableMaker;
import tanzent.cassette.theme.ThemeStore;
import tanzent.cassette.ui.adapter.holder.BaseViewHolder;
import tanzent.cassette.util.ColorUtil;
import tanzent.cassette.util.DensityUtil;

/**
 * Created by Remix on 2017/8/15.
 */

public class DesktopLyricColorAdapter extends BaseAdapter<Integer, DesktopLyricColorAdapter.FloatColorHolder> {

  //当前桌面歌词的字体颜色 默认为当前主题颜色
  private int mCurrentColor;
  private int mItemWidth;

  private static final int SIZE = DensityUtil.dip2px(18);

  public static final List<Integer> COLORS = Arrays.asList(
      R.color.md_red_primary, R.color.md_brown_primary, R.color.md_navy_primary,
      R.color.md_green_primary, R.color.md_yellow_primary, R.color.md_purple_primary,
      R.color.md_indigo_primary, R.color.md_plum_primary, R.color.md_blue_primary,
      R.color.md_white_primary, R.color.md_pink_primary
  );

  public DesktopLyricColorAdapter(Context Context, int layoutId, int width) {
    super(Context, layoutId);
    setData(COLORS);
    mItemWidth = width / COLORS.size();
    //宽度太小
    if (mItemWidth < DensityUtil.dip2px(mContext, 20)) {
      mItemWidth = DensityUtil.dip2px(mContext, 20);
    }
    mCurrentColor = ThemeStore.getFloatLyricTextColor();

  }

  /**
   * 判断是否是选中的颜色
   */
  private boolean isColorChoose(int colorRes) {
    return mContext.getResources().getColor(colorRes) == mCurrentColor;
  }

  public void setCurrentColor(int color) {
    mCurrentColor = color;
    ThemeStore.saveFloatLyricTextColor(color);
  }


  @NonNull
  @Override
  public FloatColorHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    FloatColorHolder holder = new FloatColorHolder(
        LayoutInflater.from(mContext).inflate(R.layout.item_float_lrc_color, parent, false));

    RelativeLayout.LayoutParams imgLayoutParam = new RelativeLayout.LayoutParams(
        DensityUtil.dip2px(mContext, 18), DensityUtil.dip2px(mContext, 18));
    imgLayoutParam.addRule(RelativeLayout.CENTER_IN_PARENT);
    holder.mColor.setLayoutParams(imgLayoutParam);

    RecyclerView.LayoutParams rootLayoutParam = new RecyclerView.LayoutParams(mItemWidth,
        ViewGroup.LayoutParams.MATCH_PARENT);
    holder.mRoot.setLayoutParams(rootLayoutParam);

    return holder;
  }


  @Override
  protected void convert(FloatColorHolder holder, Integer colorRes, final int position) {
    final int color = colorRes != R.color.md_white_primary ?
        ColorUtil.getColor(colorRes) : Color.parseColor("#F9F9F9");

    if (isColorChoose(colorRes)) {
      holder.mColor.setBackground(new GradientDrawableMaker()
          .shape(GradientDrawable.OVAL)
          .color(color)
          .strokeSize(DensityUtil.dip2px(1))
          .strokeColor(Color.BLACK)
          .width(SIZE)
          .height(SIZE)
          .make()
      );
    } else {
      holder.mColor.setBackground(new GradientDrawableMaker()
          .shape(GradientDrawable.OVAL)
          .color(color)
          .width(SIZE)
          .height(SIZE)
          .make());
    }
    holder.mRoot.setOnClickListener(v -> mOnItemClickListener.onItemClick(v, position));
  }

  static class FloatColorHolder extends BaseViewHolder {

    @BindView(R.id.item_color)
    ImageView mColor;

    //        @BindView(R.id.item_color_bg)
//        SimpleDraweeView mColorBg;
    public FloatColorHolder(View itemView) {
      super(itemView);
    }
  }
}
