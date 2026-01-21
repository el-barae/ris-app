package com.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "modality")
public class ModalityProperties {
    
    private String aetitle = "ORTH_MOD";
    private String host = "localhost";
    private int port = 4243;
}
