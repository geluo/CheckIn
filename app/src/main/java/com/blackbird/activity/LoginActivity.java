package com.blackbird.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.blackbird.bean.NetParam;
import com.blackbird.utils.Constant;
import com.blackbird.utils.HttpUtils;
import com.blackbird.utils.PrefUtils;
import com.blackbird.utils.ProgressUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private EditText useEdit;
    private EditText pwdEdit;
    private MyHandler mHandler = new MyHandler(this);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        useEdit = (EditText)findViewById(R.id.login_user_edit);
        pwdEdit = (EditText)findViewById(R.id.login_pwd_edit);
        String token = PrefUtils.getFromPrefs(this,"token","");
        if (!token.isEmpty())
        {
            startActivity(new Intent(this,EventListActivity.class));
            finish();
        }
    }

    /***
     * 登录
     */
    private void toLogin(String userid,String psw){
        ProgressUtil.showProgressDialog(this, "正在登录，请稍后...",true);
        HttpUtils httpUtils = new HttpUtils(this,mHandler);
        ArrayList<NetParam> netParamList = new ArrayList<>();
        netParamList.add(new NetParam("userId", userid));
        netParamList.add(new NetParam("password", psw));
        httpUtils.get(Constant.LOGIN,netParamList,false,false);
    }

    public void login(View view){
        String userStr = useEdit.getText().toString().trim();
        String pwdStr = pwdEdit.getText().toString().trim();
        if (userStr.isEmpty()){
            Snackbar.make(view,"用户名不能为空",Snackbar.LENGTH_LONG).show();
            return;
        }
        if (pwdStr.isEmpty()){
            Snackbar.make(view,"密码不能为空",Snackbar.LENGTH_LONG).show();
            return;
        }

//        if (userStr.equals("admin")&&pwdStr.equals("123")){
//            Intent intent = new Intent(this,MainActivity.class);
//            startActivity(intent);
//            finish();
//        }else {
//            Snackbar.make(view,"用户名或密码错误！",Snackbar.LENGTH_LONG).show();
//        }
        toLogin(userStr,pwdStr);
    }

    private void parseJson(String json){
        ProgressUtil.closeProgressDialog();
        try {
            JSONObject obj = new JSONObject(json);
            String status = obj.getString("status");
            String func = obj.getString("func");

            if (status.equals("ok")){
                String token = obj.getString("token");
                if (func.equals("checkinLogin")){
                    PrefUtils.saveToPrefs(this,"token",token);
                    Intent intent = new Intent(LoginActivity.this,EventListActivity.class);
                    startActivity(intent);
                    finish();
                }
            }else {
                Snackbar.make(useEdit,"用户名或密码错误！",Snackbar.LENGTH_LONG).show();
            }
        }catch (JSONException e){
            e.printStackTrace();
        }

    }

    private static class MyHandler extends Handler{
        private final WeakReference<LoginActivity> mWeakReference;
        private LoginActivity mActivity;
        public MyHandler(LoginActivity activity){
            mWeakReference = new WeakReference<LoginActivity>(activity);
            mActivity = mWeakReference.get();
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mActivity==null){
                return;
            }
            if (msg.obj==null){
                ProgressUtil.closeProgressDialog();
                Snackbar.make(mActivity.pwdEdit,"连接服务器失败，登录失败！",Snackbar.LENGTH_LONG).show();
                return;
            }
            if (msg.what==HttpUtils.NETSUCCESS){
                mActivity.parseJson((String) msg.obj);
            }else if(msg.what==HttpUtils.NETNOTWORK){
                ProgressUtil.closeProgressDialog();
                Snackbar.make(mActivity.pwdEdit,"无网络连接！",Snackbar.LENGTH_LONG).show();
            }else if (msg.what==HttpUtils.NETFAIL){
                ProgressUtil.closeProgressDialog();
                Snackbar.make(mActivity.pwdEdit,"连接服务器失败！",Snackbar.LENGTH_LONG).show();
            }
        }
    }
}
