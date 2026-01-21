package com.application.dicom;

import com.application.repository.ExamRepository;
import com.application.service.ExamStatusNotificationService;
import com.application.service.ExamStatusWebSocketService;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.*;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executors;

@Configuration
public class DicomServerConfig {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamStatusNotificationService notificationService;

    @Autowired
    private ExamStatusWebSocketService webSocketService;

    private Device device;

    @PostConstruct
    public void startDicomServer() throws IOException, GeneralSecurityException {
        try {
            startDicomServerOnPort(11112);
        } catch (IOException e) {
            System.out.println("⚠️  Échec du démarrage sur le port 11112: " + e.getMessage());
            System.out.println("🔄 Tentative de démarrage sur le port 11113...");
            try {
                startDicomServerOnPort(11113);
            } catch (IOException e2) {
                System.out.println("❌ Échec du démarrage sur le port 11113: " + e2.getMessage());
                throw e2;
            }
        }
    }

    private void startDicomServerOnPort(int port) throws IOException, GeneralSecurityException {
        device = new Device("SPRING_RIS");
        ApplicationEntity ae = new ApplicationEntity("RIS_SCP");
        Connection conn = new Connection();

        device.addConnection(conn);
        device.addApplicationEntity(ae);
        ae.setAssociationAcceptor(true);
        ae.addConnection(conn);

        // Configuration MWL
        ae.addTransferCapability(new TransferCapability(
                null,
                UID.ModalityWorklistInformationModelFind,
                TransferCapability.Role.SCP,
                UID.ImplicitVRLittleEndian
        ));

        // Configuration MPPS
        ae.addTransferCapability(new TransferCapability(
                null,
                UID.ModalityPerformedProcedureStep,
                TransferCapability.Role.SCP,
                UID.ImplicitVRLittleEndian
        ));

        // Enregistrement des services avec WebSocket
        DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
        serviceRegistry.addDicomService(new MwlFindScp(examRepository));
        serviceRegistry.addDicomService(new MppsScp(examRepository, notificationService, webSocketService));
        ae.setDimseRQHandler(serviceRegistry);

        // Configuration Réseau
        conn.setPort(port);
        conn.setBindAddress("0.0.0.0");

        // Gestion des Threads
        device.setExecutor(Executors.newCachedThreadPool());
        device.setScheduledExecutor(Executors.newSingleThreadScheduledExecutor());

        // Démarrage
        device.bindConnections();

        System.out.println("☢️  RIS DICOM Server démarré sur 0.0.0.0:" + port + " (Accessible via 10.110.82.82)");
        System.out.println("📡 WebSocket activé pour les notifications MPPS");
    }

    @PreDestroy
    public void stop() {
        if (device != null) {
            device.unbindConnections();
            System.out.println("🛑 Serveur DICOM arrêté.");
        }
    }
}