package com.blackbird.myapplication;

import android.app.Application;
import android.content.Context;

/**
 * Created by vac on 2016/5/19.
 */
public class MyApplication extends Application{
    private static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = (MyApplication) getApplicationContext();
    }
    // 获取ApplicationContext
    public static Context getContext() {
        return instance;
    }
}
