package com.example.app.dto;

import java.sql.Timestamp;

public class TimeSlotDTO {
    private int id;
    private int centerId;
    private Timestamp startTime;
    private Timestamp endTime;
    private int capacity;
    private int bookedCount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCenterId() {
        return centerId;
    }

    public void setCenterId(int centerId) {
        this.centerId = centerId;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
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

    @Override
    public String toString() {
        return "TimeSlotDTO{" +
                "id=" + id +
                ", centerId=" + centerId +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", capacity=" + capacity +
                ", bookedCount=" + bookedCount +
                '}';
    }
}
