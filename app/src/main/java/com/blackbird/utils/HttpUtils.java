package com.blackbird.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.blackbird.bean.NetParam;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;

import org.apache.http.Header;
import org.apache.http.HttpResponse;


public class HttpUtils {

	public static final String TAG = HttpUtils.class.getSimpleName();
	private ACache aCache;
	private Context mContext;
	private Handler mHandler;
	public static final int NETFAIL = 1001;
	public static final int NETSUCCESS = 200;
	public static final int NETNOTWORK = 1003;
	
	public HttpUtils(Context mContext,Handler handler){
		aCache = ACache.get (Constant.FILECACHE,"file");
		this.mContext = mContext;
		this.mHandler = handler;
	}
	
	public void get(String url,boolean isUseCache,boolean isToCache){
		Log.d(TAG, "request url :" + url);
		if (isUseCache) {//使用缓存
			Log.d(TAG, "begin use cache ");
			String resultJson = aCache.getAsString(url);
			if (resultJson==null||resultJson.isEmpty()) {
				Log.d(TAG, " cache is null");
				//去网络获取
				if (isNetworkAvailable(mContext)) {
					getJsonFromNet(url, mHandler,isToCache);
				}else{
					mHandler.sendEmptyMessage(NETNOTWORK);//网络状况不好
				}
			}else{
				Message msg = Message.obtain();
				msg.what = NETSUCCESS;
				msg.obj = resultJson;
				mHandler.sendMessage(msg);
			}
		}else{//不使用缓存
			//去网络获取
			Log.d(TAG, "begin download json from net ");
			if (isNetworkAvailable(mContext)) {
				getJsonFromNet(url, mHandler,isToCache);
			}else{
//				Log.d(TAG, "no use cache but Net not work and try to use cache ");
//				String resultJson = aCache.getAsString(url);
//				if (resultJson==null||resultJson.isEmpty()) {
//					mHandler.sendEmptyMessage(NETNOTWORK);//网络状况不好
//				}else{
//					Message msg = Message.obtain();
//					msg.what = NETSUCCESS;
//					msg.obj = resultJson;
//					mHandler.sendMessage(msg);
//				}
				mHandler.sendEmptyMessage(NETNOTWORK);//网络状况不好
			}
		}
	}


	/**
	 * 即使不使用缓存如果在没有网的情况下，也优先在缓存中去、取
	 * @param url
	 * @param isUseCache
	 * @param isToCache
	 */
	public void getUseCache(String url,boolean isUseCache,boolean isToCache){
		Log.d(TAG, "request url :" + url);
		if (isUseCache) {//使用缓存
			Log.d(TAG, "begin use cache ");
			String resultJson = aCache.getAsString(url);
			if (resultJson==null||resultJson.isEmpty()) {
				Log.d(TAG, " cache is null");
				//去网络获取
				if (isNetworkAvailable(mContext)) {
					getJsonFromNet(url, mHandler,isToCache);
				}else{
					mHandler.sendEmptyMessage(NETNOTWORK);//网络状况不好
				}
			}else{
				Message msg = Message.obtain();
				msg.what = NETSUCCESS;
				msg.obj = resultJson;
				mHandler.sendMessage(msg);
			}
		}else{//不使用缓存
			//去网络获取
			Log.d(TAG, "begin download json from net ");
			if (isNetworkAvailable(mContext)) {
				getJsonFromNet(url, mHandler,isToCache);
			}else{
				Log.d(TAG, "no use cache but Net not work and try to use cache ");
				String resultJson = aCache.getAsString(url);
				if (resultJson==null||resultJson.isEmpty()) {
					mHandler.sendEmptyMessage(NETNOTWORK);//网络状况不好
				}else{
					Message msg = Message.obtain();
					msg.what = NETSUCCESS;
					msg.obj = resultJson;
					mHandler.sendMessage(msg);
				}
				mHandler.sendEmptyMessage(NETNOTWORK);//网络状况不好
			}
		}
	}
	
	public void get(String url,ArrayList<NetParam> params,boolean isUseCache,boolean isToCache){
		if (params != null && params.size() > 0) {
			int len = params.size();
			StringBuilder bulider = new StringBuilder();
			for (int i = 0; i < len; i++) {
				NetParam p = params.get(i);
				if (p == null)
					continue;
				bulider.append("&");
				bulider.append(encode(p.getKey()));
				bulider.append("=");
				bulider.append(encode(p.getValue()));
				
			}
			if (url.contains("?")) {
				url+= bulider.toString();
			}else{
				url += "?" + bulider.toString().substring(1);
			}
		}
		get(url, isUseCache, isToCache);
	}


	public void getUseCache(String url,ArrayList<NetParam> params,boolean isUseCache,boolean isToCache){
		if (params != null && params.size() > 0) {
			int len = params.size();
			StringBuilder bulider = new StringBuilder();
			for (int i = 0; i < len; i++) {
				NetParam p = params.get(i);
				if (p == null)
					continue;
				bulider.append("&");
				bulider.append(encode(p.getKey()));
				bulider.append("=");
				bulider.append(encode(p.getValue()));

			}
			if (url.contains("?")) {
				url+= bulider.toString();
			}else{
				url += "?" + bulider.toString().substring(1);
			}
		}
		getUseCache(url, isUseCache, isToCache);
	}

	/***
	 * post请求
	 * @param url
	 * @param paramArrayList
	 */
	public void post(String url,ArrayList<NetParam> paramArrayList){
		if (isNetworkAvailable(mContext)){
			AsyncHttpClient httpClient = new AsyncHttpClient();
			RequestParams params = new RequestParams();
			for (int i=0;i<paramArrayList.size();i++){
				params.put(paramArrayList.get(i).getKey(),paramArrayList.get(i).getValue());
			}
			httpClient.post(url, params, new AsyncHttpResponseHandler() {
				@Override
				public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
					super.onSuccess(statusCode, headers, responseBody);
					try {
						String result = new String(responseBody,"UTF8");
						if (result==null||result.isEmpty()) {
							mHandler.sendEmptyMessage(NETFAIL);
						}else{
							Message msg = Message.obtain();
							msg.what = NETSUCCESS;
							msg.obj = result;
							Log.d(TAG, "the json is  "+result);
							mHandler.sendMessage(msg);
						}
					}catch (UnsupportedEncodingException e){
						e.printStackTrace();
					}

				}

				@Override
				public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
					super.onFailure(statusCode, headers, responseBody, error);
					mHandler.sendEmptyMessage(NETFAIL);
				}
			});
		}else {

		}
	}
	
	public static boolean isNetworkAvailable(Context mContext) {
		Context context = mContext.getApplicationContext();
		// 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (connectivityManager == null) {
			return false;
		} else {
			// 获取NetworkInfo对象
			NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();

			if (networkInfo != null && networkInfo.length > 0) {
				for (int i = 0; i < networkInfo.length; i++) {
					System.out.println(i + "===状态==="
							+ networkInfo[i].getState());
					System.out.println(i + "===类型==="
							+ networkInfo[i].getTypeName());
					// 判断当前网络状态是否为连接状态
					if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * @description 从网络上获取json，不考虑使用缓存的情况
	 * @param url
	 * @param mHandler
	 * @param isToCache 是否将结果进行缓存
	 * @date 2016年5月16日11:26:35
	 * @author vac
	 */
	private void getJsonFromNet(final String url,final Handler mHandler,final boolean isToCache){

		AsyncHttpClient mClient = new AsyncHttpClient();
		mClient.get(url, new AsyncHttpResponseHandler(){

			@Override
			public URI getRequestURI() {
				return super.getRequestURI();
			}

			@Override
			public void onFailure(int arg0, Header[] arg1, byte[] arg2,
					Throwable arg3) {
				super.onFailure(arg0, arg1, arg2, arg3);
				mHandler.sendEmptyMessage(NETFAIL);
			}

			@Override
			public void onFinish() {
				super.onFinish();
			}

			@Override
			public void onProgress(int bytesWritten, int totalSize) {
				super.onProgress(bytesWritten, totalSize);
			}

			@Override
			public void onRetry() {
				super.onRetry();
			}

			@Override
			public void onStart() {
				super.onStart();
			}

			@Override
			public void onSuccess(int arg0, Header[] arg1, byte[] arg2) {
				super.onSuccess(arg0, arg1, arg2);
				try {
					String result = new String(arg2,"UTF8");
					if (result==null||result.isEmpty()) {
						mHandler.sendEmptyMessage(NETFAIL);
					}else{
						Message msg = Message.obtain();
						msg.what = NETSUCCESS;
						msg.obj = result;
						Log.d(TAG, "the json is  "+result);
						mHandler.sendMessage(msg);
						if (isToCache) {//是否缓存
							aCache.put(url, result);
							Log.d(TAG, "add the cache ");
						}
					}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}

		});
	}
	
	public static String encode(String s) {
		if (s == null) {
			return "";
		}
		try {
			return URLEncoder.encode(s, "UTF-8").replace("+", "%20")
					.replace("*", "%2A").replace("%7E", "~")
					.replace("#", "%23");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
