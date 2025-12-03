package com.zjsu.ybz.course.repository;

import com.zjsu.ybz.course.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {
    Optional<Course> findByCode(String code);
}




























