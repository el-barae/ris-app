package com.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dicom")
public class DicomProperties {
    
    private MwlProperties mwl = new MwlProperties();
    private ModalityProperties modality = new ModalityProperties();
    private PacsProperties pacs = new PacsProperties();
    private TimeoutProperties connection = new TimeoutProperties();
    private TimeoutProperties association = new TimeoutProperties();
    private TimeoutProperties release = new TimeoutProperties();
    
    @Data
    public static class MwlProperties {
        private String aetitle = "RIS_MWL";
        private int port = 11112;
        private String host = "0.0.0.0";
    }
    
    @Data
    public static class ModalityProperties {
        private String aetitle = "ORTH_MOD";
        private int port = 4243;
        private String host = "localhost";
    }
    
    @Data
    public static class PacsProperties {
        private String aetitle = "ORTH_PACS";
        private int port = 4242;
        private String host = "localhost";
    }
    
    @Data
    public static class TimeoutProperties {
        private int timeout = 60;
    }
    
    // Getters pour les propriétés spécifiques
    public MwlProperties getMwl() {
        return mwl;
    }
    
    public ModalityProperties getModality() {
        return modality;
    }
    
    public PacsProperties getPacs() {
        return pacs;
    }
    
    // Getters pour les timeouts spécifiques
    public TimeoutProperties getConnection() {
        return connection;
    }
    
    public TimeoutProperties getAssociation() {
        return association;
    }
    
    public TimeoutProperties getRelease() {
        return release;
    }
}
