package com.blackbird.activity.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.blackbird.activity.CheckPointListActivity;
import com.blackbird.activity.R;
import com.blackbird.bean.CheckPoint;

/**
 * Created by vac on 2016/5/16.
 */
public class PointListAdapter extends MyBaseAdapter<CheckPoint> {
    public PointListAdapter(Context mContext) {
        super(mContext);
    }

    @Override
    public long getItemId(int position) {
        return mData.get(position).getCheckpoint();
    }

    ViewHolder holder;

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = layoutInflater.inflate(R.layout.item_checkin_point, null);
//            holder.root = (LinearLayout)convertView.findViewById(R.id.item_root);
            holder.name = (TextView) convertView.findViewById(R.id.item_point_name);
            holder.num = (TextView) convertView.findViewById(R.id.item_point_num);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (mData.size()-1==position)
        {
            holder.name.setText(mData.get(position).getName());
            holder.num.setText("");
        }
        else
        {
            holder.name.setText(mData.get(position).getName());
            holder.num.setText("("+CheckPointListActivity.checkActivityNum+"/"+mData.get(position).getCheckedNum()+")");
        }
        return convertView;
    }

    private static class ViewHolder {
        private TextView name, num;
    }
}
