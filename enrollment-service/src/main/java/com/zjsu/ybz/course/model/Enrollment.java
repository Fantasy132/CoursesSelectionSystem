package com.zjsu.ybz.course.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments", indexes = {
    @Index(name = "idx_course_id", columnList = "course_id"),
    @Index(name = "idx_student_id", columnList = "student_id")
})
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "course_id", nullable = false)
    private String courseId;
    
    @Column(name = "student_id", nullable = false)
    private String studentId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;
    
    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    public Enrollment(){
        this.enrolledAt = LocalDateTime.now();
        this.status = EnrollmentStatus.ACTIVE;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getCourseId() {
        return courseId;
    }
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getStudentId() {
        return studentId;
    }
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }
    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }
    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }
}
