package com.application.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrthancStudyArrivedRequest {
    private String accessionNumber;
    private String patientName;
    private String patientId;
    private String studyUID;
    private String modality;
    private String studyDate;
    private String description;
    private String status; // "ARRIVED"
}
