package com.zjsu.ybz.course.repository;

import com.zjsu.ybz.course.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {
    List<Enrollment> findByCourseId(String courseId);
    List<Enrollment> findByStudentId(String studentId);
    boolean existsByCourseIdAndStudentId(String courseId, String studentId);
}













