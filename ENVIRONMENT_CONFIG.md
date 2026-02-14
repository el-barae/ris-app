# Environment Variables Configuration

This application uses environment variables to configure external service URLs. The following variables are available:

## Application URLs

### OHIF Viewer URL
- **Variable**: `OHIF_BASE_URL`
- **Default**: `http://localhost`
- **Description**: Base URL for the OHIF DICOM viewer
- **Usage**: Used in ReportView to construct viewer URLs

### Orthanc Worklist URL
- **Variable**: `ORTHANC_WORKLIST_BASE_URL`
- **Default**: `http://localhost:8042/worklists/`
- **Description**: Base URL for the Orthanc worklist service
- **Usage**: Used in OrthancWorklistService to send worklist entries

## Configuration Files

### .env file
Add these variables to your `.env` file:
```env
# Application URLs
OHIF_BASE_URL=http://localhost
ORTHANC_WORKLIST_BASE_URL=http://localhost:8042/worklists/
```

### application.properties
The application.properties file is configured to read from environment variables with defaults:
```properties
# Application URLs
app.ohif-base-url=${OHIF_BASE_URL:http://localhost}
app.orthanc-worklist-base-url=${ORTHANC_WORKLIST_BASE_URL:http://localhost:8042/worklists/}
```

## Implementation Details

The configuration is implemented using Spring Boot's `@ConfigurationProperties`:

1. **ApplicationProperties** class: Maps environment variables to Java properties
2. **ReportView**: Uses `app.ohif-base-url` property
3. **OrthancWorklistService**: Uses `app.orthanc-worklist-base-url` property

## Usage Examples

### Development Environment
```env
OHIF_BASE_URL=http://localhost:3000
ORTHANC_WORKLIST_BASE_URL=http://localhost:8042/worklists/
```

### Production Environment
```env
OHIF_BASE_URL=https://ohif.example.com
ORTHANC_WORKLIST_BASE_URL=https://orthanc.example.com/worklists/
```

The application will automatically pick up these variables when started, making it easy to configure different environments without code changes.
