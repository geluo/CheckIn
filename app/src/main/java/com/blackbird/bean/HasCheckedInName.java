package com.blackbird.bean;

/**
 * Created by vac on 2016/5/16.
 */
public class HasCheckedInName {
    private long activityId;

    public String getCheckinTime() {
        return checkinTime;
    }

    public void setCheckinTime(String checkinTime) {
        this.checkinTime = checkinTime;
    }

    public long getActivityId() {
        return activityId;
    }

    public void setActivityId(long activityId) {
        this.activityId = activityId;
    }

    public int getCheckinPoint() {
        return checkinPoint;
    }

    public void setCheckinPoint(int checkinPoint) {
        this.checkinPoint = checkinPoint;
    }

    public String getCheckinCode() {
        return checkinCode;
    }

    public void setCheckinCode(String checkinCode) {
        this.checkinCode = checkinCode;
    }

    private int checkinPoint;
    private String checkinCode;
    private String checkinTime;

}
