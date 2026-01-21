package com.application.controller;

import com.application.service.PatientDataCleanupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private PatientDataCleanupService cleanupService;

    @PostMapping("/cleanup-patients")
    public String cleanupDuplicatePatients() {
        try {
            cleanupService.cleanupDuplicatePatients();
            return "Patient cleanup completed successfully";
        } catch (Exception e) {
            return "Error during cleanup: " + e.getMessage();
        }
    }
}
