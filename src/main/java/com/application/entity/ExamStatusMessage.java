package com.application.entity;

import java.time.LocalDateTime;

public class ExamStatusMessage {
    private String accessionNumber;
    private String patientName;
    private String examType;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime timestamp;
    private String message;

    public ExamStatusMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ExamStatusMessage(String accessionNumber, String patientName, String examType,
                             String oldStatus, String newStatus, String message) {
        this.accessionNumber = accessionNumber;
        this.patientName = patientName;
        this.examType = examType;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // Getters et Setters
    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}