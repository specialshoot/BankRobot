package com.example.han.bankrobot.view;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.han.bankrobot.R;
import com.example.han.bankrobot.model.FolderBean;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

/**
 * Created by 轾 on 2015/11/18.
 */
public class ListImageDirPopupWindow extends PopupWindow {
    private int mWidth;
    private int mHeight;
    private View mConvertView;
    private ListView mListView;

    private List<FolderBean> mDatas;

    /**
     * 定义一个接口,供Activity回调,这是一个文件夹选中的监听器
     */
    public interface OnDirSelectedListener {
        void onSelect(FolderBean folderBean);   //将FolderBean传出去,这样其它调用此接口的Activity等类会知道调用了哪个文件夹,包括名称数量等等信息
    }

    public OnDirSelectedListener mListener;

    public void setOnDirSelectedListener(OnDirSelectedListener mListener) {
        this.mListener = mListener;
    }

    public ListImageDirPopupWindow(Context context, List<FolderBean> mDatas) {
        calWidthAndHeight(context);
        mConvertView = LayoutInflater.from(context).inflate(R.layout.list_dir, null);
        this.mDatas = mDatas;

        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);

        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);  //表示外部可以点击
        setBackgroundDrawable(new BitmapDrawable());    //添加这句话实现点击外部使其消失

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    //点击外部使其消失
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        initViews(context);
        initEvent();
    }

    private void initViews(Context context) {
        mListView = (ListView) mConvertView.findViewById(R.id.id_list_dir);
        mListView.setAdapter(new ListDirAdapter(context, mDatas));
    }

    private void initEvent() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mListener != null) {
                    mListener.onSelect(mDatas.get(position));
                }
            }
        });
    }

    /**
     * 计算popupWindow的宽度和高度
     * 得到的popupWindow高度为屏幕高度的0.7倍
     *
     * @param context
     */
    private void calWidthAndHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        mWidth = outMetrics.widthPixels;
        mHeight = (int) (outMetrics.heightPixels * 0.7); //屏幕0.7倍的高度
    }

    private class ListDirAdapter extends ArrayAdapter<FolderBean> {

        private LayoutInflater mInflater;
        private List<FolderBean> mDatas;

        public ListDirAdapter(Context context, List<FolderBean> objects) {
            super(context, 0, objects);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.list_dir_item, parent, false);
                holder.mImg = (ImageView) convertView.findViewById(R.id.id_dir_item_image);
                holder.mDirName = (TextView) convertView.findViewById(R.id.id_dir_item_name);
                holder.mDirCount = (TextView) convertView.findViewById(R.id.id_dir_item_count);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            FolderBean bean = getItem(position);
            holder.mImg.setImageResource(R.drawable.pictures_no);   //重置

            holder.mDirName.setText(bean.getName());
            holder.mDirCount.setText(bean.getCount() + "");

            return convertView;
        }

        private class ViewHolder {
            ImageView mImg;
            TextView mDirName;
            TextView mDirCount;
        }
    }
}
