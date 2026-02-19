function OnStableStudy(studyId, tags, metadata)

  local accession   = tags["AccessionNumber"] or ""
  local patientName = tags["PatientName"] or ""
  local patientId   = tags["PatientID"] or ""
  local studyUID    = tags["StudyInstanceUID"] or ""
  local modality    = tags["ModalitiesInStudy"] or tags["Modality"] or ""
  local studyDate   = tags["StudyDate"] or ""
  local description = tags["StudyDescription"] or ""

  if accession == "" then
    PrintRecursive("⚠️  AccessionNumber vide, notification ignorée pour: " .. studyUID)
    return
  end

  local payload = string.format(
    '{"accessionNumber":"%s","patientName":"%s","patientId":"%s","studyUID":"%s","modality":"%s","studyDate":"%s","description":"%s","status":"ARRIVED"}',
    accession, patientName, patientId, studyUID, modality, studyDate, description
  )

  -- Endpoint de ton RIS/backend Spring Boot
  local response = HttpPost(
    "http://host.docker.internal:8080/api/orthanc/study-arrived",
    payload,
    { ["Content-Type"] = "application/json" }
  )

  if response == nil then
    PrintRecursive("❌ Notification RIS échouée pour: " .. accession)
  else
    PrintRecursive("✅ Notification RIS OK pour: " .. accession .. " | " .. response)
  end
end
