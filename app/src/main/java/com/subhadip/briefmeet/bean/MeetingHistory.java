package com.subhadip.briefmeet.bean;

import com.google.firebase.database.Exclude;

public class MeetingHistory {
    String id;
    String userId;
    String startTime;
    String endTime;
    String subject;
    String meeting_id;

    private boolean isChecked = false;

    @Exclude
    public boolean isChecked() {
        return isChecked;
    }

    @Exclude
    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public String getMeeting_id() {
        return meeting_id;
    }

    public void setMeeting_id(String meeting_id) {
        this.meeting_id = meeting_id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
