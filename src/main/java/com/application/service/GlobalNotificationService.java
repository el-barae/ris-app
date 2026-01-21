package com.application.service;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;
import com.application.repository.ExamRepository;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.UI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GlobalNotificationService {
    
    @Autowired
    private ExamRepository examRepository;
    
    private final ConcurrentHashMap<String, AtomicInteger> statusCounts = new ConcurrentHashMap<>();
    
    public void updateExamStatusCounts() {
        List<Exam> inProgressExams = examRepository.findByStatusWithRelations(ExamStatus.IN_PROGRESS);
        List<Exam> completedExams = examRepository.findByStatusWithRelations(ExamStatus.COMPLETED);
        
        statusCounts.put("IN_PROGRESS", new AtomicInteger(inProgressExams.size()));
        statusCounts.put("COMPLETED", new AtomicInteger(completedExams.size()));
        
        notifyStatusUpdate();
    }
    
    public void notifyExamStatusChanged(Exam exam, ExamStatus oldStatus, ExamStatus newStatus) {
        updateExamStatusCounts();
        
        String message = String.format("Examen %s - %s: %s → %s", 
            exam.getPatient().getFirstName() + " " + exam.getPatient().getLastName(),
            exam.getExamType() != null ? exam.getExamType().toString() : "N/A",
            oldStatus.toString(),
            newStatus.toString()
        );
        
        showGlobalNotification(message, getNotificationDuration(newStatus));
    }
    
    public void notifyWorklistUpdated(int addedCount, int removedCount) {
        if (addedCount > 0) {
            showGlobalNotification(addedCount + " examen(s) ajouté(s) à la worklist", 3000);
        }
        if (removedCount > 0) {
            showGlobalNotification(removedCount + " examen(s) retiré(s) de la worklist", 3000);
        }
    }
    
    private void notifyStatusUpdate() {
        int inProgress = statusCounts.get("IN_PROGRESS").get();
        int completed = statusCounts.get("COMPLETED").get();
        
        if (inProgress > 0 || completed > 0) {
            String message = String.format("Examens: %d en cours, %d terminés", inProgress, completed);
            showGlobalNotification(message, 4000);
        }
    }
    
    public void showGlobalNotification(String message, int duration) {
        UI.getCurrent().access(() -> {
            Notification notification = Notification.show(message, duration, Notification.Position.TOP_CENTER);
            notification.addThemeName("info");
            notification.getElement().getClassList().add("global-notification");
            notification.getElement().getClassList().add("notification-duration-" + duration);
        });
    }
    
    private int getNotificationDuration(ExamStatus status) {
        switch (status) {
            case IN_PROGRESS:
                return 4000;
            case COMPLETED:
                return 5000;
            default:
                return 3000;
        }
    }
    
    public int getInProgressCount() {
        return statusCounts.getOrDefault("IN_PROGRESS", new AtomicInteger(0)).get();
    }
    
    public int getCompletedCount() {
        return statusCounts.getOrDefault("COMPLETED", new AtomicInteger(0)).get();
    }
}
