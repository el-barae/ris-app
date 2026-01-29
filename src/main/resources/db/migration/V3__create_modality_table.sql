CREATE TABLE modalities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aetitle VARCHAR(16) NOT NULL UNIQUE,
    nom VARCHAR(255) NOT NULL,
    description TEXT,
    marque VARCHAR(255),
    modality_type_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (modality_type_id) REFERENCES modality_types(id),
    
    INDEX idx_modalities_aetitle (aetitle),
    INDEX idx_modalities_active (is_active),
    INDEX idx_modalities_modality_type (modality_type_id)
);
