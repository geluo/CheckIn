package com.blackbird.utils;

/**
 * Created by vac on 2016/5/19.
 */
public class ACacheUtil {
    private static ACache aCache;
    public static ACache getAcacheByCheckPoint(long activityId,int checkPoint){
        aCache = ACache.get (Constant.FILECACHE,"localcheckinlist"+checkPoint);
        return aCache;
    }

    public static String getAsString(ACache cache){
        return aCache.getAsString("localCheckInName");
    }

    public static boolean removeAcacheByCheckPoint(long activityId,int checkPoint){
        return getAcacheByCheckPoint(activityId,checkPoint).remove("localCheckInName");
    }
}
