package com.application.service;

import com.application.entity.Report;

import java.util.List;

public interface ReportService {

    Report createReport(Report report);

    Report updateReport(Long id, Report report);

    void deleteReport(Long id);

    Report findById(Long id);

    Report findByExam(Long examId);

    List<Report> findByRadiologue(Long radiologueId);

    List<Report> findUnvalidatedReports();

    Report validateReport(Long id, Long radiologueId);

    List<Report> findAll();
}
