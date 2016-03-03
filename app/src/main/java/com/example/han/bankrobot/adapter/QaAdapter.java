package com.example.han.bankrobot.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.han.bankrobot.R;
import com.example.han.bankrobot.model.QA_Model;

import java.util.ArrayList;

/**
 * Created by han on 16-1-11.
 */
public class QaAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<QA_Model> qa_models;
    private View mView;
    private ViewHolder holder = null;
    private int show_item = -1;

    public QaAdapter(Context mContext, ArrayList<QA_Model> qa_models) {
        this.mContext = mContext;
        this.qa_models=qa_models;
    }

    @Override
    public int getCount() {
        return qa_models.size();
    }

    @Override
    public Object getItem(int position) {
        return qa_models.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        holder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.qa_item, parent, false);
            holder = new ViewHolder();
            holder.content = (TextView) convertView.findViewById(R.id.id_qaitem_content);
            holder.answer = (TextView) convertView.findViewById(R.id.id_qaitem_answer);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        mView = convertView;
        holder.content.setText(qa_models.get(position).getQuestion());
        if (show_item == position) {
            holder.answer.setVisibility(View.VISIBLE);
            holder.answer.setText(qa_models.get(position).getAnswer());
        } else {
            holder.answer.setVisibility(View.GONE);
        }
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show_item = position;
                notifyDataSetChanged();
            }
        });
        return convertView;
    }

    class ViewHolder {
        public TextView content;
        public TextView answer;
    }
}
