package com.application.service;

import com.application.entity.ExamStatusMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ExamStatusWebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Envoie un message WebSocket à tous les clients connectés
     */
    public void sendStatusUpdate(ExamStatusMessage message) {
        System.out.println("📡 Envoi WebSocket: " + message.getAccessionNumber() +
                " -> " + message.getNewStatus());
        messagingTemplate.convertAndSend("/topic/exam-status", message);
    }
}