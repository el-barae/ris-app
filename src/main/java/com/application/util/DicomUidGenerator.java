package com.application.util;

import java.util.UUID;

public class DicomUidGenerator {
    
    /**
     * Generate a DICOM-compliant Study Instance UID
     * Format: 1.2.276.0.7230010.3.1.2.[random1].1.[timestamp].[random2]
     */
    public static String generateStudyInstanceUID() {
        // Generate a DICOM-compliant Study Instance UID
        long timestamp = System.currentTimeMillis();
        String randomPart1 = String.valueOf(100000000L + (long) (Math.random() * 900000000L));
        String randomPart2 = String.valueOf(100000L + (long) (Math.random() * 900000L));
        return "1.2.276.0.7230010.3.1.2." + randomPart1 + ".1." + (timestamp / 1000) + "." + randomPart2;
    }
    
    /**
     * Generate a unique Accession Number
     * Format: ACC-[timestamp]-[random]
     */
    public static String generateAccessionNumber() {
        long timestamp = System.currentTimeMillis();
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ACC-" + timestamp + "-" + random;
    }
    
    /**
     * Generate a unique Order Accession Number
     * Format: ORD-[timestamp]-[random]
     */
    public static String generateOrderAccessionNumber() {
        long timestamp = System.currentTimeMillis();
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + timestamp + "-" + random;
    }
    
    /**
     * Generate a SOP Instance UID for exams
     * Format: 1.2.276.0.7230010.3.1.3.[random1].[timestamp].[random2]
     */
    public static String generateSOPInstanceUID() {
        long timestamp = System.currentTimeMillis();
        String randomPart1 = String.valueOf(100000000L + (long) (Math.random() * 900000000L));
        String randomPart2 = String.valueOf(100000L + (long) (Math.random() * 900000L));
        return "1.2.276.0.7230010.3.1.3." + randomPart1 + "." + timestamp + "." + randomPart2;
    }
}
