package com.application.service;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

//@Service
//public class ExamStatusNotificationService {
//
//    @Autowired
//    private SimpMessagingTemplate messagingTemplate;
//
//    public void notifyExamStatusUpdate(Exam exam, ExamStatus oldStatus, ExamStatus newStatus) {
//        Map<String, Object> notification = new HashMap<>();
//        notification.put("type", "EXAM_STATUS_UPDATE");
//        notification.put("timestamp", LocalDateTime.now().toString());
//        notification.put("accessionNumber", exam.getAccessionNumber());
//        notification.put("patientName", exam.getPatient() != null ?
//            exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName() : "N/A");
//        notification.put("modality", exam.getModality());
//        notification.put("oldStatus", oldStatus.toString());
//        notification.put("newStatus", newStatus.toString());
//        notification.put("examId", exam.getId());
//
//        // Envoyer à tous les clients abonnés au canal des examens
//        messagingTemplate.convertAndSend("/topic/exam-status", (Object) notification);
//
//        // Envoyer également au canal spécifique de la worklist
//        messagingTemplate.convertAndSend("/topic/worklist-updates", (Object) notification);
//
//        System.out.println("📡 Notification envoyée: " + exam.getAccessionNumber() +
//                          " - " + oldStatus + " → " + newStatus);
//    }
//
//    public void notifyExamInProgress(Exam exam) {
//        notifyExamStatusUpdate(exam, ExamStatus.SELECTED, ExamStatus.IN_PROGRESS);
//    }
//
//    public void notifyExamCompleted(Exam exam) {
//        notifyExamStatusUpdate(exam, ExamStatus.IN_PROGRESS, ExamStatus.COMPLETED);
//    }
//
//    public void notifyExamCancelled(Exam exam) {
//        notifyExamStatusUpdate(exam, ExamStatus.IN_PROGRESS, ExamStatus.CANCELLED);
//    }
//}
