package com.example.app.dto;

public class Slot {
    private int id;
    private int impfzentrumId;
    private String startTime;
    private String endTime;
    private int capacity;
    private int bookedCount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getImpfzentrumId() {
        return impfzentrumId;
    }

    public void setImpfzentrumId(int impfzentrumId) {
        this.impfzentrumId = impfzentrumId;
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

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getBookedCount() {
        return bookedCount;
    }

    public void setBookedCount(int bookedCount) {
        this.bookedCount = bookedCount;
    }
}
