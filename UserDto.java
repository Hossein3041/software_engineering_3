package com.example.app.dto;

import java.sql.Timestamp;

public class UserDto {

    private int id;
    private String email;
    private String password;
    private Integer rolleId;
    private Integer impfzentrumId;
    private Timestamp createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getRolleId() {
        return rolleId;
    }

    public void setRolleId(Integer rolleId) {
        this.rolleId = rolleId;
    }

    public Integer getImpfzentrumId() {
        return impfzentrumId;
    }

    public void setImpfzentrumId(Integer impfzentrumId) {
        this.impfzentrumId = impfzentrumId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "BenutzerDTO{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", rolleId=" + rolleId +
                ", impfzentrumId=" + impfzentrumId +
                ", createdAt=" + createdAt +
                '}';
    }
}