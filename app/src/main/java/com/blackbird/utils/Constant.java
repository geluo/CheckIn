package com.blackbird.utils;

import java.io.File;

import android.os.Environment;

public class Constant {

	public static final String FILECACHE = Environment.getExternalStorageDirectory()+ File.separator+"checkin"+File.separator+"cache";

//	public static final String ROOT_URL = "http://120.25.236.196/bike/";

	public static final String ROOT_URL = "http://client.blackbirdsport.com/";

	/**获取名单列表*/
	public static final String NAMELIST_BY_ACTIVITY = ROOT_URL+"checkin_getActivityCheckinList";

	public static final String NAMELIST_HASCHECKEDID_BY_ACTIVTY = ROOT_URL+"checkin_getActivicityCheckedByCheckpoint";

	/**批量上传已经打卡的人*/
	public static final String UPLOAD_CHECKEDIN_NAMELIST = ROOT_URL+"checkin_activityCheckinBatch";

	/**登录*/
	public static final String LOGIN = ROOT_URL+"checkin_checkinLogin";

	/**获取赛事列表*/
	public static final String GET_ACTIVTY_LIST = ROOT_URL+"checkin_getActivitiesOfCheckin";
}
