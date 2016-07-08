package com.blackbird.activity;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.blackbird.bean.CheckInName;
import com.blackbird.utils.ACache;
import com.blackbird.utils.ACacheUtil;
import com.blackbird.zxing.camera.CameraManager;
import com.blackbird.zxing.decoding.CaptureActivityHandler;
import com.blackbird.zxing.decoding.InactivityTimer;
import com.blackbird.zxing.view.ViewfinderView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {

    private static final String TAG = CaptureActivity.class.getSimpleName();
    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    private boolean vibrate;
    private MediaPlayer failPlayer;
    private boolean failBeep;
    private MediaPlayer againPlayer;
    private boolean againBeep;
    private static final float BEEP_VOLUME = 0.10f;

    private Toolbar toolbar = null;

    private static List<CheckInName> checkInNameList = new ArrayList<>();

    /**
     * 从网络上获取的已经通过的人数
     */
    private static int hasCheckedInNum = 0;

    /**
     * 本地通过的人数
     */
//    private int checkedInSuccessNum = 0;

    private DateFormat df = null;

    private long activityId = 0;

    private String checkPointName;

    Button lightButton;

    boolean open = false;
    /**
     * 缓存
     */
//    private ACache aCache = ACache.get (Constant.FILECACHE,"localcheckinlist");
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler()
    {
        public void handleMessage(android.os.Message msg)
        {
            if (msg.what == 100)
            {
                CameraManager.init(getApplication());

                hasSurface = true;
                if (inactivityTimer == null)
                {
                    inactivityTimer = new InactivityTimer(CaptureActivity.this);
                }
                toScanBarCode();
                if (open){
                    open = false;
                    openLight();
                }
            }
        }
    };

    /**
     * 签到点
     */
    private int checkpoint = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        checkpoint = getIntent().getIntExtra("checkpoint", 0);
        hasCheckedInNum = getIntent().getIntExtra("hasCheckedInNum", 0);
        activityId = getIntent().getLongExtra("activityId", 0l);
        checkPointName = getIntent().getStringExtra("checkPointName");

        CameraManager.init(getApplication());
        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
//        cancelScanButton = (Button) this.findViewById(R.id.btn_cancel_scan);
        lightButton = (Button) this.findViewById(R.id.btn_light);
        lightButton.setOnClickListener(this);
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(true);

        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (checkpoint > 0)
        {
//            String result = aCache.getAsString("localCheckInName");
            String result = ACacheUtil.getAsString(ACacheUtil.getAcacheByCheckPoint(activityId, checkpoint));
            int length = 0;
            if (result != null)
            {
                String[] result_ = result.split(",");

                for (int i = 0; i < result_.length; i++)
                {
                    if (!result_[i].isEmpty())
                    {
                        length++;
                    }
                }
                Log.v(TAG, "本地有的数据是：" + length);
                hasCheckedInNum += length;
            }
            toolbar.setTitle(checkPointName + checkInNameList.size() + "/" + hasCheckedInNum);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {

        int id = item.getItemId();
//        if (id == R.id.action_allPlayers) {
//            return true;
//        }else if (id == R.id.action_noCompletedPlayers){
//            return true;
//        }else if (id == R.id.action_completedPlayers){
//            return true;
//        }else

        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        toScanBarCode();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();
    }

    /***
     * 设置检查的名单
     *
     * @param nameList
     */
    public static void setCheckInNameList(List<CheckInName> nameList, int hasCheckedInNum_)
    {
        checkInNameList.clear();
        checkInNameList.addAll(nameList);
        hasCheckedInNum = hasCheckedInNum_;
    }

    /**
     * Handler scan result
     * @param result
     * @param barcode
     */
    public void handleDecode(Result result, Bitmap barcode)
    {
        inactivityTimer.onActivity();
        String resultString = result.getText();

        if (resultString.equals(""))
        {
            Toast.makeText(CaptureActivity.this, "扫描失败!", Toast.LENGTH_SHORT).show();
        }
        else
        {
//			System.out.println("Result:"+resultString);
//			Intent resultIntent = new Intent();
//			Bundle bundle = new Bundle();
//			bundle.putString("result", resultString);
//			resultIntent.putExtras(bundle);
//			this.setResult(RESULT_OK, resultIntent);
//            Toast.makeText(this, resultString, Toast.LENGTH_LONG).show();
//            Snackbar.make(toolbar,""+resultString,Snackbar.LENGTH_LONG).setAction("",null).show();

            if (handler != null)
            {
                handler.quitSynchronously();
                handler = null;
            }
            CameraManager.get().closeDriver();
            inactivityTimer.shutdown();
            inactivityTimer = null;
            boolean ishas = false;
            for (int i = 0; i < checkInNameList.size(); i++)
            {
                if (resultString.equals(checkInNameList.get(i).getCheckinCode()))
                {
                    checkInNameList.get(i).setIsCheckedIn(true);
                    ishas = true;

                    if (checkInNameList.get(i).getCheckinCheckpointsList().contains(checkpoint + "="))
                    {
                        playAgainSoundAndVibrate();
                        Snackbar.make(toolbar, checkInNameList.get(i).getPlayerCode() + "已经签到！",
                                Snackbar.LENGTH_LONG).setAction("", null).show();
                        Message msg = mHandler.obtainMessage(100);
                        mHandler.sendMessageDelayed(msg, 1000);
                        return;
                    }
                    playBeepSoundAndVibrate();
                    Snackbar.make(toolbar, "签到成功，号码" + checkInNameList.get(i).getPlayerCode(),
                            Snackbar.LENGTH_LONG).setAction("", null).show();
//                    String nameStr = aCache.getAsString("localCheckInName");
                    String nameStr = ACacheUtil.getAsString(ACacheUtil.getAcacheByCheckPoint(activityId, checkpoint));
                    if (nameStr == null)
                    {
                        nameStr = "";
                    }
                    Log.i(TAG, "已经存储下的" + nameStr);
                    ACache aCache = ACacheUtil.getAcacheByCheckPoint(activityId, checkpoint);
                    if (!nameStr.contains(checkInNameList.get(i).getCheckinCode()))
                    {
                        long currentTime = System.currentTimeMillis();
                        if (nameStr.isEmpty())
                        {
                            aCache.put("localCheckInName", nameStr + checkInNameList.get(i).getCheckinCode()
                                    + "|" + currentTime);
                        }
                        else
                        {
                            aCache.put("localCheckInName", nameStr + "," + checkInNameList.get(i).getCheckinCode()
                                    + "|" + currentTime);
                        }
                        hasCheckedInNum++;
                        toolbar.setTitle("第" + checkpoint + "签到点 " + checkInNameList.size() + "/" + hasCheckedInNum);

                        String checkinPointList = checkInNameList.get(i).getCheckinCheckpointsList();
                        if (checkinPointList.isEmpty())
                        {
                            checkInNameList.get(i).setCheckinCheckpointsList(checkpoint + "="
                                    + df.format(currentTime));
                        }
                        else
                        {
                            checkInNameList.get(i).setCheckinCheckpointsList(checkinPointList
                                    + "," + checkpoint + "="
                                    + df.format(currentTime));
                        }
                    }
                    else
                    {
                        playAgainSoundAndVibrate();
                        Snackbar.make(toolbar, checkInNameList.get(i).getPlayerCode() + "重复签到,请同步数据！",
                                Snackbar.LENGTH_LONG).setAction("", null).show();
                    }
                    Log.i(TAG, "最新存储的" + aCache.getAsString("localCheckInName"));
                    break;
                }
            }

            if (!ishas)
            {
               // Snackbar.make(toolbar, "无此号码！", Snackbar.LENGTH_LONG).show();
                playFailSoundAndVibrate();
                Snackbar.make(toolbar,  "没有该参赛员，请确认号码无误！",Snackbar.LENGTH_LONG).setAction("", null).show();

            }
            Message msg = mHandler.obtainMessage(100);
            mHandler.sendMessageDelayed(msg, 1000);
        }
//		CaptureActivity.this.finish();
    }

    @Override
    protected void onDestroy() {
        if (inactivityTimer != null) {
            inactivityTimer.shutdown();
            inactivityTimer = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        super.onDestroy();
    }

    private void toScanBarCode() {
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.activity_capture_surfaceView);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.removeCallback(this);
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        decodeFormats = null;
        characterSet = null;

        playBeep = true;
        failBeep = true;
        againBeep = true;
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
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

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;

    }

    private void initCamera(SurfaceHolder surfaceHolder)
    {
        try
        {
            CameraManager.get().openDriver(surfaceHolder);
        }
        catch (IOException ioe)
        {
            return;
        } catch (RuntimeException e)
        {
            return;
        }
        if (handler == null)
        {
            handler = new CaptureActivityHandler(this, decodeFormats,
                    characterSet);
        }
    }


    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public void drawViewfinder()
    {
        viewfinderView.drawViewfinder();

    }

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

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btn_light:
                openLight();
                break;
        }
    }

    public void openLight()
    {
        if (open)
        {
            CameraManager.get().closeLight();
            lightButton.setBackgroundColor(getResources().getColor(R.color.holo_red_light));
            lightButton.setText("打开闪光灯");
            open = false;
        } else
        {
            CameraManager.get().openLight();
            lightButton.setBackgroundColor(getResources().getColor(R.color.holo_green_light));
            lightButton.setText("关闭闪光灯");
            open = true;
        }
    }
}
