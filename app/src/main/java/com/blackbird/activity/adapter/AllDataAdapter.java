package com.blackbird.activity.adapter;

import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blackbird.activity.CheckPointListActivity;
import com.blackbird.activity.R;
import com.blackbird.bean.CheckInName;
import com.blackbird.utils.PrefUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class AllDataAdapter extends MyBaseAdapter<CheckInName>
{
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public AllDataAdapter(Context mContext) {
        super(mContext);
    }

    @Override
    public long getItemId(int position)
    {
        return mData.get(position).getActivityId();
    }

    ViewHolder holder;
    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup)
    {
        if (convertView==null)
        {
            holder = new ViewHolder();
            convertView = layoutInflater.inflate(R.layout.item_checkin_all,null);
            holder.id = (TextView)convertView.findViewById(R.id.item_checkin_id);
            holder.name = (TextView)convertView.findViewById(R.id.item_checkin_name);
            holder.status = (TextView)convertView.findViewById(R.id.item_checkin_status);
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)convertView.getTag();
        }
        holder.id.setText(mData.get(position).getPlayerCode());
        holder.name.setText(mData.get(position).getPlayerName());
        String checkInListStr = mData.get(position).getCheckinCheckpointsList();
        String showTextStr = "";

        String score = null;
        if (!checkInListStr.isEmpty())
        {
            String[] checkInListStr_ = checkInListStr.split(",");

            for (int i=0;i<checkInListStr_.length;i++)
            {
                String singleCheckPointStr = checkInListStr_[i];
                String checkPoint = singleCheckPointStr.substring(0,1);
                String time = singleCheckPointStr.substring(2);
                if(i==checkInListStr_.length-1)
                {
                    showTextStr+=("签到点"+checkPoint+":   "+time+"");
                }
                else
                {
                    showTextStr+=("签到点"+checkPoint+":   "+time+"\n");
                }

                if ((CheckPointListActivity.pointNum+"").equals(checkPoint) )
                {
                    try
                    {
                        Date end = simpleDateFormat.parse(time);
                        Date begin = simpleDateFormat.parse(PrefUtils.getFromPrefs(mContext,"beginTime","2016-07-09 05:00:00"));
//                         Date end = simpleDateFormat.parse("2016-07-09 14:52:34");
//                         Date begin = simpleDateFormat.parse("2016-07-09 05:00:00");
                        if (end.getTime()>begin.getTime())
                        {
                            score = "成绩为："+formatTime(end.getTime()- begin.getTime());
                        }
//                        if (end.getTime()<begin.getTime())
//                        {
//                             score = "成绩为："+formatTime(begin.getTime()- end.getTime());
//                        }
                    }
                    catch (ParseException e)
                    {
                    }
                }
            }
            if (score!=null&&!"null".equals(score))
            {
                showTextStr += "\n"+score;
            }

        }
        else
        {
            showTextStr ="";
        }



        holder.status.setText(showTextStr);

        return convertView;
    }

    public static String formatTime(Long ms) {
        Integer ss = 1000;
        Integer mi = ss * 60;
        Integer hh = mi * 60;
        Integer dd = hh * 24;

        Long day = ms / dd;
        Long hour = (ms - day * dd) / hh;
        Long minute = (ms - day * dd - hour * hh) / mi;
        Long second = (ms - day * dd - hour * hh - minute * mi) / ss;
        Long milliSecond = ms - day * dd - hour * hh - minute * mi - second * ss;

        StringBuffer sb = new StringBuffer();
        if(day > 0) {
            sb.append(day+"天");
        }
        if(hour > 0) {
            sb.append(hour+"小时");
        }
        if(minute > 0) {
            sb.append(minute+"分");
        }
        if(second > 0) {
            sb.append(second+"秒");
        }
        if(milliSecond > 0) {
            sb.append(milliSecond+"毫秒");
        }
        return sb.toString();
    }

    private static class ViewHolder
    {
        private TextView id,name,status;
    }


    private static String getTwoLength(final int data) {
        if(data < 10) {
            return "0" + data;
        } else {
            return "" + data;
        }
    }

}
