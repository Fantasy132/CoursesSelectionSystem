package com.zjsu.ybz.course.repository;

import com.zjsu.ybz.course.model.Enrollment;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class EnrollmentRepository {
    private final Map<String, Enrollment> enrollments = new ConcurrentHashMap<>();

    public List<Enrollment> findAll(){
        return new ArrayList<>(enrollments.values());
    }

    public Optional<Enrollment> findById(String id){
        return Optional.ofNullable(enrollments.get(id));
    }

    public Enrollment save(Enrollment enrollment){
        if(enrollment.getId()==null){
            enrollment.setId(UUID.randomUUID().toString());
        }
        enrollments.put(enrollment.getId(), enrollment);
        return enrollment;
    }

    public void deleteById(String id){
        enrollments.remove(id);
    }

    public List<Enrollment> findByCourseId(String courseId){
        return enrollments.values().stream()
                .filter(e -> e.getCourseId().equals(courseId))
                .collect(Collectors.toList());
    }

    public List<Enrollment> findByStudentId(String studentId){
        return enrollments.values().stream()
                .filter(e -> e.getStudentId().equals(studentId))
                .collect(Collectors.toList());
    }

    public boolean existsByCourseIdAndStudentId(String courseId, String studentId){
        return enrollments.values().stream()
                .anyMatch(e -> e.getCourseId().equals(courseId) &&
                                        e.getStudentId().equals(studentId));
    }
}













