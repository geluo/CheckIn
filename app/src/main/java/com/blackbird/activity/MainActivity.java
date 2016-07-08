package com.blackbird.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
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
import android.widget.Toast;
import com.alibaba.fastjson.JSON;
import com.blackbird.activity.adapter.NameListAdapter;
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
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * 名单列表
     */
    private ListView nameZrcListView = null;

    /**
     * 所有的名单列表
     */
    private List<CheckInName> checkInNamesList = new ArrayList<>();

    /**
     * 已经打过卡的名单列表
     */
    private List<HasCheckedInName> hasCheckedInNamesList = new ArrayList<>();

    /**
     * 适配器
     */
    private NameListAdapter mAdapter = null;

    /**
     * 下拉刷新控件
     */
    private SwipeRefreshLayout mRefreshLayout = null;

    /**
     * toolbar工具
     */
    private Toolbar toolbar = null;

    /**
     * 签到点的个数
     */
    private int checkinPointNum = 0;

    /**
     * 选中的签到点
     */
    private int hasSelectedCheckinPoint = 0;

    /**
     * 是否显示选择签到点的选项
     */
    // private boolean isShowSelectCheckPoint = false;

    private MyHandler mHandler = new MyHandler(this);

    /**
     * 菜单对象
     */
    private Menu mMenu = null;

    /**缓存*/
//    private ACache aCache = ACache.get (Constant.FILECACHE,"localcheckinlist");

    /**
     * 签到点集合
     */
    private List<CheckPoint> checkPointList = null;

    /**
     * 已经打卡的人数
     */
    private int hasCheckedInNum = 0;

    /**
     * 活动Id
     */
    private long activityId = 0l;

    public static int position = 0;

    private String token = "";

    private DateFormat df = null;

    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    private boolean vibrate;
    private MediaPlayer failPlayer;
    private boolean failBeep;
    private MediaPlayer againPlayer;
    private boolean againBeep;
    private static final float BEEP_VOLUME = 0.10f;

    private boolean tongbu;

    private boolean isFirst;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activityId = getIntent().getLongExtra("activityId", 0);
        position = getIntent().getIntExtra("position", 0);

        hasSelectedCheckinPoint = position + 1;
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
        mRefreshLayout.setColorSchemeResources(R.color.holo_blue_bright, R.color.holo_green_light, R.color.holo_orange_light, R.color.holo_red_light);
        mRefreshLayout.setOnRefreshListener(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null)
        {
            fab.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if (hasSelectedCheckinPoint > 0)
                    {
                        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                        intent.putExtra("checkpoint", hasSelectedCheckinPoint);
                        intent.putExtra("hasCheckedInNum", hasCheckedInNum);
                        intent.putExtra("activityId", activityId);
                        intent.putExtra("checkPointName", checkPointList.get(hasSelectedCheckinPoint - 1).getName());
                        startActivity(intent);
                    } else
                    {
                        Snackbar.make(nameZrcListView, "请选择签到点", Snackbar.LENGTH_LONG).setAction("", null).show();
                    }
                }
            });
        }

        FloatingActionButton fab2 = (FloatingActionButton) findViewById(R.id.fab2);
        if (fab2 != null)
        {
            fab2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCheckByHandDialog();
                }
            });
        }

        nameZrcListView = (ListView) findViewById(R.id.main_namelist_zrclistview);
        // 设置下拉刷新的样式（可选，但如果没有Header则无法下拉刷新）
        //SimpleHeader header = new SimpleHeader(this);
        //header.setTextColor(0xff0066aa);
        //header.setCircleColor(0xff33bbee);
        //nameZrcListView.setHeadable(header);

        ProgressUtil.showProgressDialog(this, "正在获取名单列表....", false);
        mRefreshLayout.setRefreshing(true);
        //isShowSelectCheckPoint = true;
        getNameList(true, true);
        // 设置加载更多的样式
//        SimpleFooter footer = new SimpleFooter(this);
//        footer.setCircleColor(0xff33bbee);
//        nameZrcListView.setFootable(footer);
        // 设置列表项出现动画
//        nameZrcListView.setItemAnimForTopIn(R.anim.topitem_in);
//        nameZrcListView.setItemAnimForBottomIn(R.anim.bottomitem_in);

        // 下拉刷新事件回调
//        nameZrcListView.setOnRefreshStartListener(new ZrcListView.OnStartListener() {
//            @Override
//            public void onStart() {
////                refresh();
//            }
//        });
        // 加载更多事件回调
//        nameZrcListView.setOnLoadMoreStartListener(new ZrcListView.OnStartListener() {
//            @Override
//            public void onStart() {
////                loadMore();
//            }
//        });

        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        mAdapter = new NameListAdapter(this);
        nameZrcListView.setAdapter(mAdapter);
    }



    /***
     * 显示手动打卡输入框
     */
    private void showCheckByHandDialog()
    {
        AlertDialog.Builder builer = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_checkin_hand, null);
        final EditText editText = (EditText) view.findViewById(R.id.dialog_checkin_edit);
        builer.setView(view).setTitle("输入编号签到").setPositiveButton("确定", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String playCodeStr = editText.getText().toString().trim();
                if (playCodeStr.isEmpty())
                {
                    Snackbar.make(nameZrcListView, "编号不能为空", Snackbar.LENGTH_LONG).show();
                    return;
                }
                playCodeStr = playCodeStr.toUpperCase();
                if (checkInNamesList.size() < 1) {
                    return;
                }

                if (checkInNamesList.get(0).getPlayerCode().length() == 5)
                {
                    switch (playCodeStr.length())
                    {
                        case 1:
                            playCodeStr = "0000" + playCodeStr;
                            break;
                        case 2:
                            playCodeStr = "000" + playCodeStr;
                            break;
                        case 3:
                            playCodeStr = "00" + playCodeStr;
                            break;
                        case 4:
                            playCodeStr = "0" + playCodeStr;
                            break;
                    }
                } else if (checkInNamesList.get(0).getPlayerCode().length() == 6)
                {
                    switch (playCodeStr.length())
                    {
                        case 1:
                            playCodeStr = "00000" + playCodeStr;
                            break;
                        case 2:
                            playCodeStr = "0000" + playCodeStr;
                            break;
                        case 3:
                            playCodeStr = "000" + playCodeStr;
                            break;
                        case 4:
                            playCodeStr = "00" + playCodeStr;
                            break;
                        case 5:
                            playCodeStr = "0" + playCodeStr;
                            break;
                    }
                }

                toCheckPlayCode(playCodeStr);
                dialog.dismiss();
            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        }
        ).create().show();
    }

    /***
     * 验证运动员id
     */
    private void toCheckPlayCode(String playerId)
    {
        int i = 0;
        for (i = 0; i < checkInNamesList.size(); i++)
        {
            if (playerId.toUpperCase().equals(checkInNamesList.get(i).getPlayerCode().toUpperCase()))
            {
//                checkInNamesList.get(i).setIsCheckedIn(true);

                if (checkInNamesList.get(i).getCheckinCheckpointsList().contains(hasSelectedCheckinPoint + "="))
                {
                    playAgainSoundAndVibrate();
                    Snackbar.make(toolbar, checkInNamesList.get(i).getPlayerCode() + "已经签到！",Snackbar.LENGTH_LONG).setAction("", null).show();
                    return;
                }

                String nameStr = ACacheUtil.getAsString(ACacheUtil.getAcacheByCheckPoint(activityId, hasSelectedCheckinPoint));
                if (nameStr == null) {
                    nameStr = "";
                }
                Log.i(TAG, "已经存储下的" + nameStr);
                ACache aCache = ACacheUtil.getAcacheByCheckPoint(activityId, hasSelectedCheckinPoint);
                if (!nameStr.contains(checkInNamesList.get(i).getCheckinCode()))
                {
                    long currentTime = System.currentTimeMillis();
                    if (nameStr.isEmpty())
                    {
                        aCache.put("localCheckInName", nameStr + checkInNamesList.get(i).getCheckinCode()+ "|" + currentTime);
                    }
                    else
                    {
                        aCache.put("localCheckInName", nameStr + "," + checkInNamesList.get(i).getCheckinCode() + "|" + currentTime);
                    }


                    toolbar.setSubtitle(checkInNamesList.size() + "/" + (hasCheckedInNum + getNotSyncNum()));

                    String checkinPointList = checkInNamesList.get(i).getCheckinCheckpointsList();

                    if (checkinPointList.isEmpty())
                    {
                        checkInNamesList.get(i).setCheckinCheckpointsList(hasSelectedCheckinPoint + "="+ df.format(currentTime));
                    }
                    else
                    {
                        checkInNamesList.get(i).setCheckinCheckpointsList(checkinPointList+ "," + hasSelectedCheckinPoint + "="+ df.format(currentTime));
                    }
                    updateSyncNumInMenu();
                    playBeepSoundAndVibrate();
                    Snackbar.make(toolbar, "签到成功，号码" + checkInNamesList.get(i).getPlayerCode(),Snackbar.LENGTH_LONG).setAction("", null).show();
                    mAdapter.notifyDataSetChanged();
                }
                else
                {

                    playAgainSoundAndVibrate();
                    Snackbar.make(toolbar, checkInNamesList.get(i).getPlayerCode() + "签到重复,请同步数据！",Snackbar.LENGTH_LONG).setAction("", null).show();
                }
                Log.i(TAG, "最新存储的" + aCache.getAsString("localCheckInName"));
                break;
            }
        }
        if (i==checkInNamesList.size())
        {
            playFailSoundAndVibrate();
            Snackbar.make(toolbar,  "没有该参赛员，请确认输入无误！",Snackbar.LENGTH_LONG).setAction("", null).show();
        }
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

    /**
     * 已经打过卡的名单列表
     */
//    private void getNameListHasCheckedIn(){
//
//        String url = Constant.NAMELIST_HASCHECKEDID_BY_ACTIVTY;
//        ArrayList<NetParam> paramsList = new ArrayList<>();
//        paramsList.add(new NetParam("activityId","1"));
//        HttpUtils httpUtils = new HttpUtils(this,mHandler);
//        httpUtils.get(url, paramsList, false, true);
//    }

    /**
     * 同步中
     */
    private void uploadHasCheckedInNameList()
    {
        if (!HttpUtils.isNetworkAvailable(this))
        {
            Snackbar.make(nameZrcListView, "请打开数据网络！", Snackbar.LENGTH_LONG).show();
            return;
        }
        ProgressUtil.showProgressDialog(this, "正在同步列表，请稍后...", true);

        for (int i = 0; i < checkPointList.size(); i++)
        {
            String result = ACacheUtil.getAsString(ACacheUtil.getAcacheByCheckPoint(activityId, (i + 1)));
            if (result != null && (!result.isEmpty()))
            {
                String url = Constant.UPLOAD_CHECKEDIN_NAMELIST;
                ArrayList<NetParam> netParamArrayList = new ArrayList<>();
                netParamArrayList.add(new NetParam("activityId", "" + activityId));
                netParamArrayList.add(new NetParam("checkinPoint", "" + (i + 1)));
                netParamArrayList.add(new NetParam("localTimestamp", "" + System.currentTimeMillis()));
                netParamArrayList.add(new NetParam("checkinList", "" + ACacheUtil
                        .getAsString(ACacheUtil.getAcacheByCheckPoint(activityId, i + 1))));
                netParamArrayList.add(new NetParam("ton", "" + token));
                HttpUtils httpUtils = new HttpUtils(this, mHandler);
                httpUtils.post(url, netParamArrayList);
            }
        }
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
//                    checkinPointNum = obj.getInt("checkinPointNum");
                    JSONArray checkPointArray = obj.getJSONArray("checkpoints");
                    if (checkPointList != null)
                    {
                        checkPointList.clear();
                    }
                    checkPointList = JSON.parseArray(checkPointArray.toString(), CheckPoint.class);
                    checkinPointNum = checkPointList.size();
                    JSONArray array = obj.getJSONArray("checkinList");
                    checkInNamesList.clear();
                    checkInNamesList = JSON.parseArray(array.toString(), CheckInName.class);
                    mAdapter.setData(checkInNamesList);
//                    getNameListHasCheckedIn();
                    CaptureActivity.setCheckInNameList(checkInNamesList, hasCheckedInNum);

                    if (hasSelectedCheckinPoint >= 1)
                    {
                        hasCheckedInNum = checkPointList.get(hasSelectedCheckinPoint - 1).getCheckedNum();
                    }

//                    toolbar.setSubtitle("全部"+mAdapter.getCount()+"人/已打卡"+hasCheckedInNum+"人");
                    toolbar.setSubtitle(mAdapter.getCount() + "/" + hasCheckedInNum);

                }
//                else if (func.equals("getActivicityCheckedByCheckpoint")){
//                    JSONArray array = obj.getJSONArray("checkedList");
//                    hasCheckedInNamesList.clear();
//                    for (int i=0;i<array.length();i++){
//                        HasCheckedInName hasCheckedInName = new HasCheckedInName();
//                        JSONObject object = array.getJSONObject(i);
//                        hasCheckedInName.setActivityId(object.getLong("activityId"));
//                        JSONObject object1 = object.getJSONObject("id");
//                        hasCheckedInName.setCheckinPoint(object1.getInt("checkinPoint"));
//                        hasCheckedInName.setCheckinCode(object1.getString("checkinCode"));
//                        hasCheckedInName.setCheckinTime(object.getString("checkinTime"));
//                        hasCheckedInNamesList.add(hasCheckedInName);
//                    }
//                    hasCheckedInNum = 0;
//                    for (int i=0;i<hasCheckedInNamesList.size();i++){
//                        for (int j=0;j<checkInNamesList.size();j++){
//                            if (checkInNamesList.get(j).getCheckinCode().
//                                    equals(hasCheckedInNamesList.get(i).getCheckinCode())){
//                                Log.i("TAG", checkInNamesList.get(j).getCheckinCode() + "," +
//                                        checkInNamesList.get(j).getPlayerName());
//                                checkInNamesList.get(j).setIsCheckedIn(true);
//                                hasCheckedInNum++;
//                                break;
//                            }
//                        }
//                    }
//                    mAdapter.setData(checkInNamesList);
//                    CaptureActivity.setCheckInNameList(checkInNamesList, hasCheckedInNum);
//                    toolbar.setSubtitle("全部"+mAdapter.getCount()+"人/已打卡"+hasCheckedInNum+"人");
//                }
                else if (func.equals("activityCheckinBatch"))
                {
                    ProgressUtil.closeProgressDialog();
                    Snackbar.make(nameZrcListView, "同步成功！", Snackbar.LENGTH_LONG).setAction("", null).show();
                    mMenu.getItem(0).setTitle("同步(0)");
//                    aCache.remove("localCheckInName");
                    ACacheUtil.removeAcacheByCheckPoint(activityId, hasSelectedCheckinPoint);
                    ProgressUtil.showProgressDialog(this, "正在拉取最新签到数据...", true);
                    onRefresh();
                }
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

    }

    /***
     * 选择签到点
     */
    private void selectCheckPont()
    {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        final CharSequence[] choices = new CharSequence[checkinPointNum];
//        for (int i = 0; i < checkinPointNum; i++) {
//            choices[i] = checkPointList.get(i).getName();
//        }
//        builder.setItems(choices, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                hasSelectedCheckinPoint = (which + 1);
//                dialog.dismiss();
//                Snackbar.make(nameZrcListView, "已选择" + checkPointList.get(which).getName(), Snackbar.LENGTH_SHORT
//                ).setAction("", null).show();
//
//                mMenu.getItem(1).setTitle(choices[which]);
//                hasCheckedInNum = checkPointList.get(which).getCheckedNum();
//                toolbar.setSubtitle(mAdapter.getCount() + "/" + hasCheckedInNum);
//                updateSyncNumInMenu();
//            }
//        }).setTitle("选择签到点").setCancelable(true).create().show();

        Toast.makeText(this, "这是" + mMenu.getItem(1).getTitle() + "打卡处", Toast.LENGTH_SHORT).show();
    }

    private void handResult(Message msg)
    {
//        ProgressUtil.closeProgressDialog();
        mRefreshLayout.setRefreshing(false);

        if (msg.what == HttpUtils.NETSUCCESS)
        {
//                nameZrcListView.setRefreshSuccess();
            parseJson((String) msg.obj);

            if (isFirst)
            {
                Snackbar.make(nameZrcListView, "获取参赛名单成功！", Snackbar.LENGTH_LONG).show();
                isFirst = false;
            }
            else if(tongbu)
            {
                //Snackbar.make(nameZrcListView, "上传成功！", Snackbar.LENGTH_LONG).show();
                Toast.makeText(this,"上传成功！",Toast.LENGTH_LONG).show();
                tongbu = false;
            }
            else
            {
                Snackbar.make(nameZrcListView, "刷新成功！", Snackbar.LENGTH_LONG).show();
            }

//            if (isShowSelectCheckPoint) {
//              //  selectCheckPont();
////                toolbar.setSubtitle("全部" + mAdapter.getCount() + "人/已打卡0人");
//                toolbar.setSubtitle(mAdapter.getCount() + "人/0");
//                isShowSelectCheckPoint = false;
//            }
            toolbar.setSubtitle(mAdapter.getCount() + "/" + hasCheckedInNum);

        }
        else
        {
//                nameZrcListView.setRefreshFail();
            Snackbar.make(nameZrcListView, "刷新失败！", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (mMenu != null)
        {
//            String result = aCache.getAsString("localCheckInName");
            updateSyncNumInMenu();
        } else
        {
            Log.i("TAG", "mMenu is null ----");
        }
        playBeep = true;
        failBeep = true;
        againBeep = true;
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        int max = audioService.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioService.getStreamVolume(AudioManager.STREAM_MUSIC);
        audioService.setStreamVolume(AudioManager.STREAM_MUSIC, max, 0); //tempVolume:音量绝对值
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL)
        {
            playBeep = false;
            failBeep = false;
            againBeep = false;
        }

        initAgainBeepSound();
        initBeepSound();
        initFailBeepSound();
        vibrate = true;
    }

    /***
     * 更新菜单的同步数量
     */
    private void updateSyncNumInMenu()
    {

        int num = getNotSyncNum();
        if (num > 0)
        {
            mMenu.getItem(0).setTitle("同步(" + num + ")");
        }
        else
        {
            mMenu.getItem(0).setTitle("同步(0)");
        }
    }

    /**
     * 返回本地的数量
     *
     * @return 本地的数量
     */
    private int getNotSyncNum()
    {
        String result = ACacheUtil.getAsString(ACacheUtil.getAcacheByCheckPoint(activityId, hasSelectedCheckinPoint));
        if (result != null)
        {
            String[] result_ = result.split(",");
            return result_.length;
        }
        else
        {
            return 0;
        }
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
//        String result = aCache.getAsString("localCheckInName");
        String result = ACacheUtil.getAsString(ACacheUtil.getAcacheByCheckPoint(activityId, hasSelectedCheckinPoint));

        if (result != null)
        {
            String[] result_ = result.split(",");
//            toolbar.setSubtitle("全部"+mAdapter.getCount()+"人/已打卡"+(hasCheckedInNum+result_.length)+"人");
            toolbar.setSubtitle(mAdapter.getCount() + "/" + (hasCheckedInNum + result_.length));
        }
        mAdapter.notifyDataSetChanged();
    }


    @Override
    public void onRefresh()
    {
//        isShowSelectCheckPoint = false;
//        String result = aCache.getAsString("localCheckInName");
        String result = ACacheUtil.getAsString(ACacheUtil.getAcacheByCheckPoint(activityId, hasSelectedCheckinPoint));
        if (result != null)
        {
            Snackbar.make(nameZrcListView, "请先同步数据，再进行刷新！", Snackbar.LENGTH_LONG).show();
            mRefreshLayout.setRefreshing(false);
        }
        else
        {
            getNameList(false, true);
        }

//        if (mAdapter!=null&&mAdapter.getCount()>0){
////            getNameListHasCheckedIn();
//            getNameList();
//        }else if (mAdapter!=null){
//            getNameList();
////            getNameListHasCheckedIn();
//        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (mediaPlayer != null)
        {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (failPlayer != null)
        {
            failPlayer.stop();
            failPlayer.release();
            failPlayer = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenu = menu;
//        String result = aCache.getAsString("localCheckInName");
        String result = ACacheUtil.getAsString(ACacheUtil.getAcacheByCheckPoint(activityId, hasSelectedCheckinPoint));
        if (result != null)
        {
            String[] result_ = result.split(",");
            mMenu.getItem(0).setTitle("同步(" + result_.length + ")");
        }
        else
        {
            mMenu.getItem(0).setTitle("同步(0)");
        }

        final CharSequence[] choices = new CharSequence[checkinPointNum];

        for (int i = 0; i < checkinPointNum; i++)
        {
            choices[i] = checkPointList.get(i).getName();
        }
        mMenu.getItem(1).setTitle(choices[position]);
        hasCheckedInNum = checkPointList.get(position).getCheckedNum();
        toolbar.setSubtitle(mAdapter.getCount() + "/" + hasCheckedInNum);
        updateSyncNumInMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_allPlayers) {
//            return true;
//        }else if (id == R.id.action_noCompletedPlayers){
//            return true;
//        }else if (id == R.id.action_completedPlayers){
//            return true;
//        }else

        if (id == R.id.action_sync)
        {
            if (item.getTitle().equals("同步(0)"))
            {
                Snackbar.make(nameZrcListView, "无本地同步数据！", Snackbar.LENGTH_LONG).setAction("", null).show();
            }
            else
            {
                tongbu = true;
                uploadHasCheckedInNameList();
            }
            return true;
        }
        else if (id == R.id.action_checkpoint)
        {
            selectCheckPont();
        }
        else if (id == android.R.id.home)
        {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }


    private static class MyHandler extends Handler
    {
        private final WeakReference<MainActivity> mWeakReference;

        private MainActivity mActivity;

        public MyHandler(MainActivity activity)
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


//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event)
//    {
//        return true;
//    }

    private void initBeepSound()
    {
        if (playBeep && mediaPlayer == null)
        {
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(beepListener);

            AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.sign_success);
            try
            {
                mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
                file.close();
                mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mediaPlayer.prepare();
            }
            catch (IOException e)
            {
                mediaPlayer = null;
            }
        }
    }

    private void initFailBeepSound()
    {
        if (failBeep && failPlayer == null)
        {
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            failPlayer = new MediaPlayer();
            failPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            failPlayer.setOnCompletionListener(beepListener);

            AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.sign_fail);
            try
            {
                failPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
                file.close();
                failPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                failPlayer.prepare();
            }
            catch (IOException e)
            {
                failPlayer = null;
            }
        }
    }

    private void initAgainBeepSound()
    {
        if (againBeep && againPlayer == null)
        {
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            againPlayer = new MediaPlayer();
            againPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            againPlayer.setOnCompletionListener(beepListener);

            AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.sign_again);
            try
            {
                againPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
                file.close();
                againPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                againPlayer.prepare();
            }
            catch (IOException e)
            {
                againPlayer = null;
            }
        }
    }


    private static final long VIBRATE_DURATION = 200L;

    private void playBeepSoundAndVibrate()
    {
        if (playBeep && mediaPlayer != null)
        {
            mediaPlayer.start();
        }
        if (vibrate)
        {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    private void playFailSoundAndVibrate()
    {
        if (failBeep && failPlayer != null)
        {
            failPlayer.start();
        }
        if (vibrate)
        {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    private void playAgainSoundAndVibrate()
    {
        if (againBeep && againPlayer != null)
        {
            againPlayer.start();
        }
        if (vibrate)
        {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    private final MediaPlayer.OnCompletionListener beepListener = new MediaPlayer.OnCompletionListener()
    {
        public void onCompletion(MediaPlayer mediaPlayer)
        {
            mediaPlayer.seekTo(0);
        }
    };
}