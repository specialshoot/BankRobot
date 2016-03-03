package com.example.han.bankrobot.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.han.bankrobot.R;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by 轾 on 2015/11/18.
 */
public class VideoAdapter extends BaseAdapter {

    public static Set<String> mSeletedImg = new HashSet<String>();   //集合保存图片选中的状态
    private String mDirPath;
    private List<String> mImgPaths;
    private LayoutInflater mInflater;

    public VideoAdapter(Context context, List<String> mDatas, String dirPath) {
        this.mImgPaths = mDatas;
        this.mDirPath = dirPath;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mImgPaths.size();
    }

    @Override
    public Object getItem(int position) {
        return mImgPaths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.grid_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.mImg = (ImageView) convertView.findViewById(R.id.id_item_image);
            viewHolder.mImg.setColorFilter(null);
            viewHolder.mSelect = (ImageButton) convertView.findViewById(R.id.id_item_select);
            viewHolder.mName = (TextView) convertView.findViewById(R.id.id_item_name);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.mImg.setImageResource(R.drawable.pictures_no);
        viewHolder.mSelect.setImageResource(R.drawable.picture_unselected);
        viewHolder.mName.setText(mImgPaths.get(position));

        final String filePath = mDirPath + "/" + mImgPaths.get(position);
        viewHolder.mImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSeletedImg.contains(filePath)) {
                    //已经被选择,清除选择状态
                    mSeletedImg.remove(filePath);
                    viewHolder.mImg.setColorFilter(null);
                    viewHolder.mSelect.setImageResource(R.drawable.picture_unselected);
                } else {
                    //还未被选择
                    mSeletedImg.add(filePath);
                    viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));
                    viewHolder.mSelect.setImageResource(R.drawable.pictures_selected);
                }
            }
        });

        return convertView;
    }

    private class ViewHolder {
        ImageView mImg;
        ImageButton mSelect;
        TextView mName;
    }
}
