# Order Entity Documentation

## Overview
The Order entity has been created to manage medical imaging orders that can contain one or more exams. This entity establishes relationships between hospitals, doctors, and exams.

## Entity Structure

### Order Entity
**Table**: `orders`

**Attributes**:
- `id` (Long) - Primary key
- `studyInstanceUID` (String) - Unique Study Instance UID
- `accessionNumber` (String) - Unique Accession Number
- `hospital` (Hospital) - Many-to-one relationship with Hospital
- `doctor` (User) - Many-to-one relationship with User (doctor)
- `exams` (List<Exam>) - One-to-many relationship with Exam
- `createdAt` (LocalDateTime) - Creation timestamp
- `updatedAt` (LocalDateTime) - Last update timestamp

### Relationships

#### Order ↔ Hospital
- **Type**: Many-to-One
- **Description**: An order belongs to one hospital, a hospital can have multiple orders
- **Foreign Key**: `hospital_id` in orders table

#### Order ↔ User (Doctor)
- **Type**: Many-to-One
- **Description**: An order is assigned to one doctor, a doctor can have multiple orders
- **Foreign Key**: `doctor_id` in orders table

#### Order ↔ Exam
- **Type**: One-to-Many
- **Description**: An order can contain one or more exams, an exam belongs to one order
- **Foreign Key**: `order_id` in exams table

## Database Schema

```sql
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    study_instance_uid VARCHAR(255) NOT NULL UNIQUE,
    accession_number VARCHAR(255) NOT NULL UNIQUE,
    hospital_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id),
    FOREIGN KEY (doctor_id) REFERENCES users(id)
);

-- Add foreign key to exams table
ALTER TABLE exams ADD COLUMN order_id BIGINT;
ALTER TABLE exams ADD FOREIGN KEY (order_id) REFERENCES orders(id);
```

## Repository Methods

### OrderRepository
- `findByAccessionNumber(String accessionNumber)` - Find order by accession number
- `findByStudyInstanceUID(String studyInstanceUID)` - Find order by Study Instance UID
- `findByHospitalId(Long hospitalId)` - Find all orders for a hospital
- `findByDoctorId(Long doctorId)` - Find all orders for a doctor
- `findByHospitalAndDoctor(Long hospitalId, Long doctorId)` - Find orders by both hospital and doctor
- `findByExamId(Long examId)` - Find order containing a specific exam
- `existsByAccessionNumber(String accessionNumber)` - Check if accession number exists
- `existsByStudyInstanceUID(String studyInstanceUID)` - Check if Study Instance UID exists

## Service Methods

### OrderService
- `findAll()` - Get all orders
- `findById(Long id)` - Get order by ID
- `createOrder(Order order)` - Create new order (validates uniqueness)
- `updateOrder(Order order)` - Update existing order
- `deleteOrder(Long id)` - Delete order
- `addExamToOrder(Long orderId, Exam exam)` - Add exam to order
- `removeExamFromOrder(Long orderId, Long examId)` - Remove exam from order
- `findByExamId(Long examId)` - Find order by exam ID
- `getTotalOrdersCount()` - Get total number of orders
- `getOrdersByHospitalCount(Long hospitalId)` - Get order count by hospital
- `getOrdersByDoctorCount(Long doctorId)` - Get order count by doctor

## Usage Examples

### Creating a new Order
```java
Order order = new Order();
order.setStudyInstanceUID("1.2.840.10008.5.1.4.1.1.1.123456");
order.setAccessionNumber("ACC20240214001");
order.setHospital(hospital);
order.setDoctor(doctor);

Order savedOrder = orderService.createOrder(order);
```

### Adding exams to an Order
```java
Order order = orderService.findById(orderId).orElse(null);
if (order != null) {
    order.addExam(exam1);
    order.addExam(exam2);
    orderService.updateOrder(order);
}
```

### Finding Orders
```java
// Find by hospital
List<Order> hospitalOrders = orderService.findByHospital(hospitalId);

// Find by doctor
List<Order> doctorOrders = orderService.findByDoctor(doctorId);

// Find order containing specific exam
Optional<Order> order = orderService.findByExamId(examId);
```

## Validation Rules
- `studyInstanceUID` must be unique across all orders
- `accessionNumber` must be unique across all orders
- Both `hospital` and `doctor` are required (not null)
- Exams can be added or removed from orders dynamically

## Integration with Existing System
- The Exam entity has been updated to include an `order` field
- Existing exams can be associated with orders without breaking existing functionality
- The relationship is optional - exams can exist without being part of an order
