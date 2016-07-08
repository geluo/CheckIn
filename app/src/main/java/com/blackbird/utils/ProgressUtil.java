package com.blackbird.utils;

/**
 * Created by vac on 2016/5/16.
 */
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
public class ProgressUtil {
    private static ProgressDialog dialog;
    /**
     * 显示进度对话框
     * @param context
     * @param msg
     */
    public static void showProgressDialog(Context context,String msg,boolean cancleable){
        if(dialog==null){
            dialog=new ProgressDialog(context);
            dialog.setCancelable(cancleable);
            dialog.setMessage(msg);
            dialog.show();
        }
    }
    /**
     * 关闭进度对话框
     */
    public static void closeProgressDialog(){
        if(dialog!=null){
            dialog.cancel();
            dialog=null;
        }
    }
}