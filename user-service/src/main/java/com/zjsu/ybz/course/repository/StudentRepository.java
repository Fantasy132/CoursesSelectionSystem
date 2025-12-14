package com.zjsu.ybz.course.repository;

import com.zjsu.ybz.course.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    Optional<Student> findByStudentId(String studentId);
    Optional<Student> findByEmail(String email);
    boolean existsByStudentId(String studentId);
    boolean existsByStudentIdAndIdNot(String studentId, String id);
}


















