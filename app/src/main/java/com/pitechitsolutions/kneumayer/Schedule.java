package com.pitechitsolutions.kneumayer;

public class Schedule {
    private String userName;
    private String date;
    private String clockInTime;
    private String clockOutTime;
    private String photoPath; // New field

    // Constructor
    // Constructor
    public Schedule(String userName, String date, String clockInTime, String clockOutTime, String photoPath) {
        this.userName = userName;
        this.date = date;
        this.clockInTime = clockInTime;
        this.clockOutTime = clockOutTime;
        this.photoPath = photoPath; // Initialize new field
    }

    // Getters
    public String getUserName() {
        return userName;
    }

    public String getDate() {
        return date;
    }

    public String getClockInTime() {
        return clockInTime;
    }

    public String getClockOutTime() {
        return clockOutTime;
    }

    // Setters
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setClockInTime(String clockInTime) {
        this.clockInTime = clockInTime;
    }

    public void setClockOutTime(String clockOutTime) {
        this.clockOutTime = clockOutTime;
    }

    public String getPhotoPath() { // New getter
        return photoPath;
    }
}
