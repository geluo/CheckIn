package com.blackbird.activity.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.blackbird.activity.R;
import com.blackbird.bean.Event;

/**
 * Created by vac on 2016/5/19.
 */
public class EventListAdapter extends MyBaseAdapter<Event>
{

    public EventListAdapter(Context mContext) {
        super(mContext);
    }

    @Override
    public long getItemId(int position) {
        return mData.get(position).getActivityId();
    }

    ViewHolder holder;
    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup)
    {
        if (convertView==null)
        {
            convertView =layoutInflater.inflate(R.layout.item_event_list,null);
            holder = new ViewHolder();
            holder.name = (TextView)convertView.findViewById(R.id.item_event_list_name);
            holder.startTime = (TextView)convertView.findViewById(R.id.item_event_list_starttime);
            holder.endTime = (TextView)convertView.findViewById(R.id.item_event_list_endtime);
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)convertView.getTag();
        }
        holder.name.setText(mData.get(position).getTitle());
        holder.startTime.setText("开始时间："+mData.get(position).getStartTime());
        holder.endTime.setText("结束时间："+mData.get(position).getEndTime());
        return convertView;
    }
    private static class ViewHolder
    {
        private TextView name,startTime,endTime;
    }
}
