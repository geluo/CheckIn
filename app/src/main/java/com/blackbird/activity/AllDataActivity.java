package com.blackbird.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;

import com.alibaba.fastjson.JSON;
import com.blackbird.activity.adapter.AllDataAdapter;
import com.blackbird.bean.CheckInName;
import com.blackbird.bean.CheckPoint;
import com.blackbird.bean.HasCheckedInName;
import com.blackbird.bean.NetParam;
import com.blackbird.utils.ACache;
import com.blackbird.utils.ACacheUtil;
import com.blackbird.utils.Constant;
import com.blackbird.utils.HttpUtils;
import com.blackbird.utils.PrefUtils;
import com.blackbird.utils.ProgressUtil;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class AllDataActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener
{

    private static final String TAG = AllDataActivity.class.getSimpleName();

    /**名单列表*/
    private ListView nameZrcListView = null;

    /**所有的名单列表*/
    private List<CheckInName> checkInNamesList = new ArrayList<>();

    /**已经打过卡的名单列表*/
    private List<HasCheckedInName> hasCheckedInNamesList = new ArrayList<>();

    /**适配器*/
    private AllDataAdapter mAdapter = null;

    /**下拉刷新控件*/
    private SwipeRefreshLayout mRefreshLayout = null;

    /**toolbar工具*/
    private Toolbar toolbar = null;

    /**签到点的个数*/
    private int checkinPointNum = 0;

    /**选中的签到点*/
    private int hasSelectedCheckinPoint = 0;

    /**是否显示选择签到点的选项*/
    private boolean isShowSelectCheckPoint = false;

    private MyHandler mHandler = new MyHandler(this);

    /**缓存*/
//    private ACache aCache = ACache.get (Constant.FILECACHE,"localcheckinlist");

    /**签到点集合*/
    private List<CheckPoint> checkPointList = null;

    /**已经打卡的人数*/
    private int hasCheckedInNum = 0;

    /**活动Id*/
    private long activityId = 0l;

    private String token = "";

    private DateFormat df = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all);
        activityId = getIntent().getLongExtra("activityId",0);
        token = PrefUtils.getFromPrefs(this, "token", "");
        if (token.equals("")){
            startActivity(new Intent(this,LoginActivity.class));
            finish();
        }
        initView();
    }

    private void initView(){
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.main_swipeRefresh);
        mRefreshLayout.setColorSchemeResources(
                R.color.holo_blue_bright, R.color.holo_green_light,
                R.color.holo_orange_light, R.color.holo_red_light);

        mRefreshLayout.setOnRefreshListener(this);

        nameZrcListView = (ListView)findViewById(R.id.main_namelist_zrclistview);

        ProgressUtil.showProgressDialog(this,"正在获取名单列表....",false);
        mRefreshLayout.setRefreshing(true);
        isShowSelectCheckPoint =true;
        getNameList(true,true);

        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        mAdapter = new AllDataAdapter(this);
        nameZrcListView.setAdapter(mAdapter);
    }


    /***
     * 所有名单列表
     */
    private void getNameList(boolean isUseCache,boolean needToSaveCache)
    {
        String url = Constant.NAMELIST_BY_ACTIVITY;
        ArrayList<NetParam> paramsList = new ArrayList<>();
        paramsList.add(new NetParam("activityId", ""+activityId));
        paramsList.add(new NetParam("ton",token));
        HttpUtils httpUtils = new HttpUtils(this,mHandler);
        httpUtils.getUseCache(url, paramsList,isUseCache,needToSaveCache);
    }

    private void parseJson(String json){
        try{
            System.out.println("result="+json);
            ProgressUtil.closeProgressDialog();
            JSONObject obj = new JSONObject(json);
            String status = obj.getString("status");
            String func = obj.getString("func");
            if (status.equals("ok")){
                if (func.equals("getActivityCheckinList"))
                {
                    JSONArray checkPointArray = obj.getJSONArray("checkpoints");
                    if(checkPointList!=null)
                    {
                        checkPointList.clear();
                    }

                    checkPointList = JSON.parseArray(checkPointArray.toString(),CheckPoint.class);
                    checkinPointNum = checkPointList.size();

                    JSONArray array = obj.getJSONArray("checkinList");
                    checkInNamesList.clear();
                    checkInNamesList = JSON.parseArray(array.toString(), CheckInName.class);
                    mAdapter.setData(checkInNamesList);
                    CaptureActivity.setCheckInNameList(checkInNamesList,hasCheckedInNum);

                    if (hasSelectedCheckinPoint>=1)
                    {
                        hasCheckedInNum = checkPointList.get(hasSelectedCheckinPoint-1).getCheckedNum();
                    }
//                    toolbar.setSubtitle(mAdapter.getCount()+"/"+hasCheckedInNum);
                }
                else if (func.equals("activityCheckinBatch")){
                    ProgressUtil.closeProgressDialog();
                    Snackbar.make(nameZrcListView,"同步成功！",Snackbar.LENGTH_LONG).setAction("", null).show();

                    ProgressUtil.showProgressDialog(this, "正在拉取最新签到数据...",true);
                    onRefresh();
                }
            }
        }catch (JSONException e){
            e.printStackTrace();
        }

    }

    private void handResult(Message msg){
//        ProgressUtil.closeProgressDialog();
        mRefreshLayout.setRefreshing(false);

        if (msg.what==HttpUtils.NETSUCCESS){
//                nameZrcListView.setRefreshSuccess();
            parseJson((String) msg.obj);
            Snackbar.make(nameZrcListView, "刷新成功！", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();

            if (isShowSelectCheckPoint){
//                toolbar.setSubtitle(mAdapter.getCount() + "人/0");
                isShowSelectCheckPoint = false;
            }

        }else{
//                nameZrcListView.setRefreshFail();
            Snackbar.make(nameZrcListView, "刷新失败！", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }



    @Override
    protected void onRestart() {
        super.onRestart();
//        String result = aCache.getAsString("localCheckInName");
        String result = ACacheUtil.getAsString(ACacheUtil.getAcacheByCheckPoint(activityId,hasSelectedCheckinPoint));
        if (result!=null){
            String[] result_ = result.split(",");
//            toolbar.setSubtitle("全部"+mAdapter.getCount()+"人/已打卡"+(hasCheckedInNum+result_.length)+"人");
           // toolbar.setSubtitle(mAdapter.getCount()+"/"+(hasCheckedInNum+result_.length));
        }

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRefresh() {
        isShowSelectCheckPoint = false;
//        String result = aCache.getAsString("localCheckInName");
        String result = ACacheUtil.getAsString(ACacheUtil.getAcacheByCheckPoint(activityId,hasSelectedCheckinPoint));
        if (result!=null){
            Snackbar.make(nameZrcListView,"刷新成功！",Snackbar.LENGTH_LONG).show();
            mRefreshLayout.setRefreshing(false);
        }else {
            getNameList(false,true);
        }
//        Snackbar.make(nameZrcListView,"刷新成功！",Snackbar.LENGTH_LONG).show();
    }




    private static class MyHandler extends Handler
    {
        private final WeakReference<AllDataActivity> mWeakReference;
        private AllDataActivity  mActivity;
        public MyHandler(AllDataActivity activity){
            mWeakReference  = new WeakReference<>(activity);
            mActivity = mWeakReference.get();
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(mActivity==null){
                return;
            }
            mActivity.handResult(msg);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId()== android.R.id.home)
        {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}