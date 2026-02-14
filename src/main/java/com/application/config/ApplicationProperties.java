package com.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {
    
    private String ohifBaseUrl;
    private String orthancWorklistBaseUrl;
    
    public String getOhifBaseUrl() {
        return ohifBaseUrl;
    }
    
    public void setOhifBaseUrl(String ohifBaseUrl) {
        this.ohifBaseUrl = ohifBaseUrl;
    }
    
    public String getOrthancWorklistBaseUrl() {
        return orthancWorklistBaseUrl;
    }
    
    public void setOrthancWorklistBaseUrl(String orthancWorklistBaseUrl) {
        this.orthancWorklistBaseUrl = orthancWorklistBaseUrl;
    }
}
