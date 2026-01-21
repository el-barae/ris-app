package com.application.service;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;

import java.time.LocalDate;
import java.util.List;

public interface ExamService {

    Exam createExam(Exam exam);

    Exam updateExam(Long id, Exam exam);

    void deleteExam(Long id);

    Exam findById(Long id);

    List<Exam> findAll();

    List<Exam> findByPatient(Long patientId);

    List<Exam> findByStatus(ExamStatus status);

    List<Exam> findByMedecin(Long medecinId);

    List<Exam> findScheduledExams(LocalDate date);

    Exam updateStatus(Long id, ExamStatus newStatus);

    String generateAccessionNumber();

    String generateStudyInstanceUID();
}
