package com.blackbird.bean;

/**
 * Created by vac on 2016/5/16.
 */
public class CheckInName {
    private long activityId;
    private String playerPhone;
    private String checkinCheckpointsList;
    private String playerCode;
    private String playerName;
    private String checkinCode;

    private boolean isCheckedIn = false;

    public String getCheckinCode() {
        return checkinCode;
    }

    public void setCheckinCode(String checkinCode) {
        this.checkinCode = checkinCode;
    }

    public long getActivityId() {
        return activityId;
    }

    public void setActivityId(long activityId) {
        this.activityId = activityId;
    }

    public String getPlayerCode() {
        return playerCode;
    }

    public void setPlayerCode(String playerCode) {
        this.playerCode = playerCode;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public boolean isCheckedIn() {
        return isCheckedIn;
    }

    public void setIsCheckedIn(boolean isCheckedIn) {
        this.isCheckedIn = isCheckedIn;
    }

    public String getPlayerPhone() {
        return playerPhone;
    }

    public void setPlayerPhone(String playerPhone) {
        this.playerPhone = playerPhone;
    }

    public String getCheckinCheckpointsList() {
        return checkinCheckpointsList;
    }

    public void setCheckinCheckpointsList(String checkinCheckpointsList) {
        this.checkinCheckpointsList = checkinCheckpointsList;
    }
}
