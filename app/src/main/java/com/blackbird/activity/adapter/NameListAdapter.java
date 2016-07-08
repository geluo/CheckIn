package com.blackbird.activity.adapter;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.blackbird.activity.MainActivity;
import com.blackbird.activity.R;
import com.blackbird.bean.CheckInName;
import com.blackbird.utils.MyDialog;

/**
 * Created by vac on 2016/5/16.
 */
public class NameListAdapter extends MyBaseAdapter<CheckInName>
{
    public Context context ;
    public NameListAdapter(Context mContext)
    {
        super(mContext);
        context = mContext;
    }

    @Override
    public long getItemId(int position) {
        return mData.get(position).getActivityId();
    }

    ViewHolder holder;

    MyDialog myDialog;
    @Override
    public View getView(final int position, View convertView, ViewGroup viewGroup)
    {
        if (convertView==null)
        {
            holder = new ViewHolder();
            convertView = layoutInflater.inflate(R.layout.item_checkin_name,null);
//            holder.root = (LinearLayout)convertView.findViewById(R.id.item_root);
            holder.id = (TextView)convertView.findViewById(R.id.item_checkin_id);
            holder.name = (TextView)convertView.findViewById(R.id.item_checkin_name);
            holder.status = (TextView)convertView.findViewById(R.id.item_checkin_status);
            holder.button = (ImageView)convertView.findViewById(R.id.phone);
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)convertView.getTag();
        }
        holder.id.setText(mData.get(position).getPlayerCode());
        holder.name.setText(mData.get(position).getPlayerName());
//        if (mData.get(position).isCheckedIn()){
//            holder.root.setBackgroundResource(R.color.holo_blue_bright);
//        }else{
//            holder.root.setBackgroundResource(R.color.white);
//        }
        String checkInListStr = mData.get(position).getCheckinCheckpointsList();
        String showTextStr = "";
        if (!checkInListStr.isEmpty())
        {
            String[] checkInListStr_ = checkInListStr.split(",");
            if (checkInListStr_!=null&&checkInListStr_.length>0)
            {
                for (int i=0;i<checkInListStr_.length;i++)
                {
                    String singleCheckPointStr = checkInListStr_[i];
                    String checkPoint = singleCheckPointStr.substring(0,1);
                    String time = singleCheckPointStr.substring(2);

                    if ((MainActivity.position+1+"").equals(checkPoint))
                    {
                        showTextStr+=(time);
                        break;
                    }
                }
            }
            else
            {
                showTextStr ="";
            }
        }
        else
        {
            showTextStr ="";
        }
        holder.status.setText(showTextStr);

        holder.button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                System.out.println(mData.get(position));
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_CALL);
                String phoneNumber =  mData.get(position).getPlayerPhone();

                if (phoneNumber!=null&&!"".equals(phoneNumber))
                {
                    initPhone(phoneNumber);
                    myDialog.setCanceledOnTouchOutside(false);
                    if (!myDialog.isShowing())
                    {
                        myDialog.show();
                    }
                }
                else
                {
                    Toast.makeText(mContext,"该参赛选手号码为空！",Toast.LENGTH_SHORT).show();
                }
            }
        });
        return convertView;
    }

     public void initPhone(final String phoneString)
     {
         final Intent intent = new Intent(Intent.ACTION_CALL,Uri.parse("tel:" + phoneString));
         myDialog = new MyDialog(mContext, R.style.MyDialog,
                "拨号："+phoneString+"？", "取消", "确定",
                new MyDialog.DialogClickListener()
                {
                    @Override
                    public void onRightBtnClick(Dialog dialog)
                    {
                        context.startActivity(intent);
                        dialog.dismiss();
                    }

                    @Override
                    public void onLeftBtnClick(Dialog dialog) {
                        dialog.dismiss();
                    }
                });
    }


    private static class ViewHolder
    {
        private TextView id,name,status;
        private ImageView button;
    }
}
