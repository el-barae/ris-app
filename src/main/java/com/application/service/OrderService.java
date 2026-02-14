package com.application.service;

import com.application.entity.Order;
import com.application.entity.Exam;
import com.application.entity.Hospital;
import com.application.entity.User;
import com.application.repository.OrderRepository;
import com.application.repository.ExamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ExamRepository examRepository;

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> findByAccessionNumber(String accessionNumber) {
        return orderRepository.findByAccessionNumber(accessionNumber);
    }

    public Optional<Order> findByStudyInstanceUID(String studyInstanceUID) {
        return orderRepository.findByStudyInstanceUID(studyInstanceUID);
    }

    public List<Order> findByHospital(Long hospitalId) {
        return orderRepository.findByHospitalId(hospitalId);
    }

    public List<Order> findByDoctor(Long doctorId) {
        return orderRepository.findByDoctorId(doctorId);
    }

    public List<Order> findByHospitalAndDoctor(Long hospitalId, Long doctorId) {
        return orderRepository.findByHospitalAndDoctor(hospitalId, doctorId);
    }

    public Order createOrder(Order order) {
        logger.info("Creating new order with accession number: {}", order.getAccessionNumber());
        
        // Validate unique constraints
        if (orderRepository.existsByAccessionNumber(order.getAccessionNumber())) {
            throw new IllegalArgumentException("Order with accession number " + order.getAccessionNumber() + " already exists");
        }
        
        if (orderRepository.existsByStudyInstanceUID(order.getStudyInstanceUID())) {
            throw new IllegalArgumentException("Order with Study Instance UID " + order.getStudyInstanceUID() + " already exists");
        }
        
        Order savedOrder = orderRepository.save(order);
        logger.info("Successfully created order with ID: {}", savedOrder.getId());
        return savedOrder;
    }

    public Order updateOrder(Order order) {
        logger.info("Updating order with ID: {}", order.getId());
        
        if (!orderRepository.existsById(order.getId())) {
            throw new IllegalArgumentException("Order with ID " + order.getId() + " not found");
        }
        
        Order updatedOrder = orderRepository.save(order);
        logger.info("Successfully updated order with ID: {}", updatedOrder.getId());
        return updatedOrder;
    }

    public void deleteOrder(Long id) {
        logger.info("Deleting order with ID: {}", id);
        
        if (!orderRepository.existsById(id)) {
            throw new IllegalArgumentException("Order with ID " + id + " not found");
        }
        
        orderRepository.deleteById(id);
        logger.info("Successfully deleted order with ID: {}", id);
    }

    public Order addExamToOrder(Long orderId, Exam exam) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Order with ID " + orderId + " not found");
        }
        
        Order order = orderOpt.get();
        order.addExam(exam);
        
        return orderRepository.save(order);
    }

    public Order removeExamFromOrder(Long orderId, Long examId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Order with ID " + orderId + " not found");
        }
        
        Optional<Exam> examOpt = examRepository.findById(examId);
        if (examOpt.isEmpty()) {
            throw new IllegalArgumentException("Exam with ID " + examId + " not found");
        }
        
        Order order = orderOpt.get();
        order.removeExam(examOpt.get());
        
        return orderRepository.save(order);
    }

    public Optional<Order> findByExamId(Long examId) {
        return orderRepository.findByExamId(examId);
    }

    public long getTotalOrdersCount() {
        return orderRepository.count();
    }

    public long getOrdersByHospitalCount(Long hospitalId) {
        return orderRepository.findByHospitalId(hospitalId).size();
    }

    public long getOrdersByDoctorCount(Long doctorId) {
        return orderRepository.findByDoctorId(doctorId).size();
    }
}
