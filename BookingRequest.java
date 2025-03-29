package com.example.app.dto;

public class BookingRequest {
    private int centerId;
    private String appointmentDate;
    private int slotId;
    private String bookingFor;
    private int vaccineId;
    private int patientCount;

    public int getCenterId() {
        return centerId;
    }

    public void setCenterId(int centerId) {
        this.centerId = centerId;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(String appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public int getSlotId() {
        return slotId;
    }

    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }

    public String getBookingFor() {
        return bookingFor;
    }

    public void setBookingFor(String bookingFor) {
        this.bookingFor = bookingFor;
    }

    public int getVaccineId() {
        return vaccineId;
    }

    public void setVaccineId(int vaccineId) {
        this.vaccineId = vaccineId;
    }

    public int getPatientCount() {
        return patientCount;
    }

    public void setPatientCount(int patientCount) {
        this.patientCount = patientCount;
    }

    @Override
    public String toString() {
        return "BookingRequest{" +
                "centerId=" + centerId +
                ", appointmentDate='" + appointmentDate + '\'' +
                ", slotId=" + slotId +
                ", bookingFor='" + bookingFor + '\'' +
                ", vaccineId=" + vaccineId +
                ", patientCount=" + patientCount +
                '}';
    }
}