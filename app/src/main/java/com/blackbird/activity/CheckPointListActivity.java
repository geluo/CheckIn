package com.blackbird.activity;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.alibaba.fastjson.JSON;
import com.blackbird.activity.adapter.PointListAdapter;
import com.blackbird.bean.CheckInName;
import com.blackbird.bean.CheckPoint;
import com.blackbird.bean.NetParam;
import com.blackbird.utils.Constant;
import com.blackbird.utils.HttpUtils;
import com.blackbird.utils.MyDialog;
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


public class CheckPointListActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener
{

    private static final String TAG = CheckPointListActivity.class.getSimpleName();

    private Toolbar toolbar = null;

    /**
     * 名单列表
     */
    private ListView nameZrcListView = null;
    /**
     * 所有的名单列表
     */
    private List<CheckInName> checkInNamesList = new ArrayList<>();
    /**
     * 适配器
     */
    private PointListAdapter mAdapter = null;
    /**
     * 下拉刷新控件
     */
    private SwipeRefreshLayout mRefreshLayout = null;
    /**
     * 签到点的个数
     */
    private int checkinPointNum = 0;

    private MyHandler mHandler = new MyHandler(this);
    /**缓存*/
    //    private ACache aCache = ACache.get (Constant.FILECACHE,"localcheckinlist");
    /**
     * 签到点集合
     */
    private List<CheckPoint> checkPointList = null;
    /**
     * 活动Id
     */
    private long activityId = 0l;

    private String token = "";

    private DateFormat df = null;

    public static int checkActivityNum = 0;

    private boolean isFirst ;

    public static int pointNum = 0;

    MyDialog myDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_point);
        activityId = getIntent().getLongExtra("activityId", 0);
        token = PrefUtils.getFromPrefs(this, "token", "");
        if (token.equals(""))
        {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
        initView();
        isFirst = true;
    }

    private void initView()
    {

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.main_swipeRefresh);
        mRefreshLayout.setColorSchemeResources(R.color.holo_blue_bright, R.color.holo_green_light,R.color.holo_orange_light, R.color.holo_red_light);

        mRefreshLayout.setOnRefreshListener(this);
        nameZrcListView = (ListView) findViewById(R.id.main_namelist_zrclistview);
        // 设置下拉刷新的样式（可选，但如果没有Header则无法下拉刷新）
        //        SimpleHeader header = new SimpleHeader(this);
        //        header.setTextColor(0xff0066aa);
        //        header.setCircleColor(0xff33bbee);
        //        nameZrcListView.setHeadable(header);

        ProgressUtil.showProgressDialog(this, "正在获取签到点列表....", false);
        mRefreshLayout.setRefreshing(true);
        getNameList(true, true);
        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        mAdapter = new PointListAdapter(this);
        nameZrcListView.setAdapter(mAdapter);

        nameZrcListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == checkPointList.size()-1){
                    Intent intent = new Intent(CheckPointListActivity.this, AllDataActivity.class);
                    intent.putExtra("activityId",activityId);
                    startActivity(intent);
                }
                else
                {
//                    Intent intent = new Intent(CheckPointListActivity.this, MainActivity.class);
//                    intent.putExtra("position",position);
//                    intent.putExtra("activityId",activityId);
//                    startActivity(intent);
                    initPhone(position);
                    myDialog.setCanceledOnTouchOutside(false);
                    if (!myDialog.isShowing())
                    {
                        myDialog.show();
                    }
                }
            }
        });
    }

    public void initPhone(final int position)
    {
        myDialog = new MyDialog(this, R.style.MyDialog,
                "您确认进入  "+checkPointList.get(position).getName()+"  列表吗？", "取消", "确定",
                new MyDialog.DialogClickListener()
                {
                    @Override
                    public void onRightBtnClick(Dialog dialog)
                    {
                        Intent intent = new Intent(CheckPointListActivity.this, MainActivity.class);
                        intent.putExtra("activityId",activityId);
                        intent.putExtra("position",position);
                        startActivity(intent);
                        dialog.dismiss();
                    }

                    @Override
                    public void onLeftBtnClick(Dialog dialog) {
                        dialog.dismiss();
                    }
                });
    }


    /***
     * 所有名单列表
     */
    private void getNameList(boolean isUseCache, boolean needToSaveCache)
    {
        String url = Constant.NAMELIST_BY_ACTIVITY;
        ArrayList<NetParam> paramsList = new ArrayList<>();
        paramsList.add(new NetParam("activityId", "" + activityId));
        paramsList.add(new NetParam("ton", token));
        HttpUtils httpUtils = new HttpUtils(this, mHandler);
        httpUtils.getUseCache(url, paramsList, isUseCache, needToSaveCache);
    }

    private void parseJson(String json)
    {
        try {
            System.out.println("result=" + json);
            ProgressUtil.closeProgressDialog();
            JSONObject obj = new JSONObject(json);
            String status = obj.getString("status");
            String func = obj.getString("func");
            if (status.equals("ok"))
            {
                if (func.equals("getActivityCheckinList"))
                {
                    //checkinPointNum = obj.getInt("checkinPointNum");
                    JSONArray checkPointArray = obj.getJSONArray("checkpoints");
                    if (checkPointList != null)
                    {
                        checkPointList.clear();
                    }
                    checkPointList = JSON.parseArray(checkPointArray.toString(), CheckPoint.class);
                    JSONArray array = obj.getJSONArray("checkinList");
                    checkInNamesList.clear();
                    checkInNamesList = JSON.parseArray(array.toString(), CheckInName.class);
                    checkActivityNum = checkInNamesList.size();
                    if (checkPointList.size()<1)
                    {
                        Snackbar.make(nameZrcListView, "该赛事还没有签到点！", Snackbar.LENGTH_LONG).show();
                    }
                    else
                    {
                        pointNum = checkPointList.size();
                        CheckPoint checkPoint = new CheckPoint();
                        checkPoint.setName("查看总表");
                        checkPointList.add(checkPoint);
                    }

                    mAdapter.setData(checkPointList);
                }
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }



    private void handResult(Message msg)
    {
        mRefreshLayout.setRefreshing(false);

        if (msg.what == HttpUtils.NETSUCCESS)
        {
            parseJson((String) msg.obj);

            if (isFirst)
            {
                Snackbar.make(nameZrcListView, "获取签到点成功！", Snackbar.LENGTH_LONG).show();
                isFirst = false;
            }
            else
            {
                Snackbar.make(nameZrcListView, "刷新成功！", Snackbar.LENGTH_LONG).show();
            }
        } else {
            Snackbar.make(nameZrcListView, "刷新失败！", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
        }
    }

    @Override
    public void onRefresh()
    {

        getNameList(false, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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


    private static class MyHandler extends Handler
    {
        private final WeakReference<CheckPointListActivity> mWeakReference;
        private CheckPointListActivity mActivity;

        public MyHandler(CheckPointListActivity activity)
        {
            mWeakReference = new WeakReference<>(activity);
            mActivity = mWeakReference.get();
        }

        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            if (mActivity == null)
            {
                return;
            }
            mActivity.handResult(msg);
        }
    }
}