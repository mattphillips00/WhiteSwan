package org.example;

public class Race {
    private String meetingName;
    private String meetingId;
    private String raceNumber;
    private String raceTime;
    private String url;

    public Race(String meetingId, String raceTime, String url) {
        this.meetingId = meetingId;
        this.raceTime = raceTime;
        this.url = url;
    }

    public String getRaceTime() {
        return raceTime;
    }

    public void setRaceTime(String raceTime) {
        this.raceTime = raceTime;
    }

    public String getRaceNumber() {
        return raceNumber;
    }

    public void setRaceNumber(String raceNumber) {
        this.raceNumber = raceNumber;
    }

    public String getMeetingName() {
        return meetingName;
    }

    public void setMeetingName(String meetingName) {
        this.meetingName = meetingName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    @Override
    public String toString() {
        return "Race{" +
            "meetingName='" + meetingName + '\'' +
            ", raceNumber='" + raceNumber + '\'' +
            ", raceTime='" + raceTime + '\'' +
            ", url='" + url + '\'' +
            '}';
    }
}