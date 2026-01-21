package com.application.service;

import org.springframework.stereotype.Service;

@Service
public class MWLService {
    
    private boolean running = false;
    
    public MWLService() {
        // Service implementation for Modality Worklist management
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void startServer() {
        this.running = true;
    }
    
    public void stopServer() {
        this.running = false;
    }
    
    // Add methods for MWL operations as needed
}
