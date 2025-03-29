package com.example.app.dto;

public class PatientDTO {
    private int id;
    private int benutzerId;

    public PatientDTO() {
    }

    public PatientDTO(int id, int benutzerId) {
        this.id = id;
        this.benutzerId = benutzerId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBenutzerId() {
        return benutzerId;
    }

    public void setBenutzerId(int benutzerId) {
        this.benutzerId = benutzerId;
    }

    @Override
    public String toString() {
        return "PatientDTO{" +
                "id=" + id +
                ", benutzerId=" + benutzerId +
                '}';
    }
}