package com.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "pacs")
public class PacsProperties {
    
    private String aetitle = "ORTH_PACS";
    private String host = "localhost";
    private int port = 4242;
}
