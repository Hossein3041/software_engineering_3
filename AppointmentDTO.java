package com.example.app.dto;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class AppointmentDTO {
    private int id;
    private int patientId;
    private int timeSlotId;
    private int vaccineId;
    private String status;
    private Timestamp createdAt;
    private Timestamp cancelledAt;
    private String qrCode;

    private Timestamp startTime;
    private Timestamp endTime;
    private String patientName;
    private String centerName;
    private String vaccineName;

    private Map<String, Object> additionalInfo = new HashMap<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public int getTimeSlotId() {
        return timeSlotId;
    }

    public void setTimeSlotId(int timeSlotId) {
        this.timeSlotId = timeSlotId;
    }

    public int getVaccineId() {
        return vaccineId;
    }

    public void setVaccineId(int vaccineId) {
        this.vaccineId = vaccineId;
    }

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
        additionalInfo.put("centerName", centerName);
    }

    public String getVaccineName() {
        return vaccineName;
    }

    public void setVaccineName(String vaccineName) {
        this.vaccineName = vaccineName;
        additionalInfo.put("vaccineName", vaccineName);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Timestamp cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public void setAdditionalInfo(String key, Object value) {
        additionalInfo.put(key, value);

        if ("startTime".equals(key) && value instanceof Timestamp) {
            this.startTime = (Timestamp) value;
        } else if ("endTime".equals(key) && value instanceof Timestamp) {
            this.endTime = (Timestamp) value;
        } else if ("patientName".equals(key) && value instanceof String) {
            this.patientName = (String) value;
        } else if ("centerName".equals(key) && value instanceof String) {
            this.centerName = (String) value;
        } else if ("vaccineName".equals(key) && value instanceof String) {
            this.vaccineName = (String) value;
        }
    }

    public Object getAdditionalInfo(String key) {
        return additionalInfo.get(key);
    }

    public Map<String, Object> getAllAdditionalInfo() {
        return additionalInfo;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
        additionalInfo.put("startTime", startTime);
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
        additionalInfo.put("endTime", endTime);
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
        additionalInfo.put("patientName", patientName);
    }

    @Override
    public String toString() {
        return "AppointmentDTO{" +
                "id=" + id +
                ", patientId=" + patientId +
                ", timeSlotId=" + timeSlotId +
                ", vaccineId=" + vaccineId +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", cancelledAt=" + cancelledAt +
                ", qrCode='" + qrCode + '\'' +
                ", additionalInfo=" + additionalInfo +
                '}';
    }
}