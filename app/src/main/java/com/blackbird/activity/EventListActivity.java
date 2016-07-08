package com.blackbird.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.alibaba.fastjson.JSON;
import com.blackbird.activity.adapter.EventListAdapter;
import com.blackbird.bean.Event;
import com.blackbird.bean.NetParam;
import com.blackbird.utils.ACacheUtil;
import com.blackbird.utils.Constant;
import com.blackbird.utils.HttpUtils;
import com.blackbird.utils.PrefUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventListActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener
{

    private SwipeRefreshLayout mSwipeRefresh;
    private ListView eventListView;
    private MyHandler mHandler = new MyHandler(this);
    private EventListAdapter mAdapter = null;
    private boolean isFirst ;

    /**
     * 菜单对象
     */
    private Menu mMenu = null;

    List<Event> eventList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        eventListView = (ListView) findViewById(R.id.event_list_listview);
        mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.event_list_swipeRefresh);
        mSwipeRefresh.setOnRefreshListener(this);

        mAdapter = new EventListAdapter(this);
        eventListView.setAdapter(mAdapter);

        eventListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Intent intent = new Intent(EventListActivity.this, CheckPointListActivity.class);
                intent.putExtra("activityId", mAdapter.getItemId(position));
                PrefUtils.saveToPrefs(EventListActivity.this,"beginTime",eventList.get(position).getStartTime());
                startActivity(intent);
            }
        });
        mSwipeRefresh.post(new Runnable()
        {
            @Override
            public void run()
            {
                mSwipeRefresh.setRefreshing(true);
            }
        });
        this.onRefresh();

        isFirst = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_logout, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {

        switch (item.getItemId())
        {
            case R.id.action_logout:
                logout();
                break;
        }

        return true;
    }

    public void logout()
    {
        PrefUtils.saveToPrefs(this, "token", null);
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onRefresh() {
        mHandler.postDelayed(new Runnable()
        {
            @Override
            public void run() {
                getEventList();
            }
        }, 500);
    }

    private void getEventList()
    {
        HttpUtils httpUtils = new HttpUtils(this, mHandler);
        ArrayList<NetParam> params = new ArrayList<>();
        String token = PrefUtils.getFromPrefs(this, "token", "");
        if (!token.isEmpty())
        {
            params.add(new NetParam("ton", token));
            httpUtils.getUseCache(Constant.GET_ACTIVTY_LIST, params, false, true);
        }
        else
        {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void parseJson(String json)
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Long current = new Date().getTime()/1000;
        try {

            if (isFirst)
            {
                Snackbar.make(eventListView, "获取赛事成功！", Snackbar.LENGTH_LONG).show();
                isFirst = false;
            }
            else
            {
                Snackbar.make(eventListView, "刷新成功！", Snackbar.LENGTH_LONG).show();
            }
            JSONObject object = new JSONObject(json);
            String status = object.getString("status");
            String func = object.getString("func");
            if (status.equals("ok") && func.equals("getActivitiesOfCheckin"))
            {
                JSONArray array = object.getJSONArray("activities");
                 eventList= JSON.parseArray(array.toString(), Event.class);

                //判断赛事是否过期
                for (int i=0;i<eventList.size();i++)
                {
                    try {
                        Date date = simpleDateFormat.parse(eventList.get(i).getEndTime());
                        if (date.getTime()/1000<current)
                        {
                            eventList.remove(i);
                        }
                    }
                    catch (ParseException e)
                    {
                    }
                }
                if (eventList.size()<1)
                {
                    Snackbar.make(eventListView, "您当前没有赛事信息！", Snackbar.LENGTH_LONG).show();
                }
                mAdapter.setData(eventList);
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private static class MyHandler extends Handler
    {
        private final WeakReference<EventListActivity> mWeakReference;
        private EventListActivity mActivity;

        public MyHandler(EventListActivity activity)
        {
            mWeakReference = new WeakReference<EventListActivity>(activity);
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
            mActivity.mSwipeRefresh.setRefreshing(false);
            if (msg.obj == null)
            {
                Snackbar.make(mActivity.eventListView, "连接服务器失败！", Snackbar.LENGTH_LONG).show();
                return;
            }
            if (msg.what == HttpUtils.NETSUCCESS)
            {
                mActivity.parseJson((String) msg.obj);
            }
            else if (msg.what == HttpUtils.NETNOTWORK)
            {
                Snackbar.make(mActivity.eventListView, "无网络连接！", Snackbar.LENGTH_LONG).show();
            }
            else if (msg.what == HttpUtils.NETFAIL)
            {
                Snackbar.make(mActivity.eventListView, "连接服务器失败！", Snackbar.LENGTH_LONG).show();
            }
        }
    }
}
