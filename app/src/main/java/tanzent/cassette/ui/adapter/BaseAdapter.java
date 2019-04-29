package tanzent.cassette.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import tanzent.cassette.misc.interfaces.OnItemClickListener;
import tanzent.cassette.ui.adapter.holder.BaseViewHolder;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/19 11:31
 */
public abstract class BaseAdapter<D, T extends BaseViewHolder> extends RecyclerView.Adapter<T> {

  protected Context mContext;
  protected OnItemClickListener mOnItemClickListener;
  List<D> mDatas;
  protected int mLayoutId;
  private Constructor mConstructor;


  public BaseAdapter(Context context, int layoutId) {
    this.mContext = context;
    this.mLayoutId = layoutId;
    try {
      this.mConstructor = getGenericClass().getDeclaredConstructor(View.class);
      this.mConstructor.setAccessible(true);
    } catch (Exception e) {
      throw new IllegalArgumentException(e.toString());
    }
  }

  public void setData(List<D> datas) {
    mDatas = datas;
    notifyDataSetChanged();
  }

  public List<D> getDatas() {
    return mDatas;
  }

  @Override
  public void onBindViewHolder(T holder, int position) {
    convert(holder, getItem(position), position);
  }

  protected D getItem(int position) {
    return mDatas.get(position);
  }

  protected abstract void convert(final T holder, D d, final int position);

  @SuppressWarnings("unchecked")
  @Override
  public T onCreateViewHolder(ViewGroup parent, int viewType) {
    try {
      View itemView = LayoutInflater.from(mContext).inflate(mLayoutId, parent, false);
      return (T) mConstructor.newInstance(itemView);
    } catch (Exception e) {
      throw new IllegalArgumentException(e.toString());
    }
  }

  public void setOnItemClickListener(OnItemClickListener l) {
    this.mOnItemClickListener = l;
  }

  @Override
  public int getItemCount() {
    return mDatas != null ? mDatas.size() : 0;
  }

  @SuppressWarnings("unchecked")
  protected final Class<T> getGenericClass() {
    Type genType = getClass().getGenericSuperclass();
    Type[] params = ((ParameterizedType) genType).getActualTypeArguments();

    if (params != null && params.length > 1 && params[1] instanceof Class<?>) {
      return (Class<T>) params[1];
    } else {
      throw new IllegalArgumentException("Generic error");
    }
  }
}
