package com.zjsu.ybz.course.repository;

import com.zjsu.ybz.course.model.Student;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class StudentRepository {
    private final Map<String, Student> students = new ConcurrentHashMap<>();

    public List<Student> findAll(){
        return new ArrayList<>(students.values());
    }

    public Optional<Student> findById(String id){
        return Optional.ofNullable(students.get(id));
    }

    public Student save(Student student){
        if(student.getId()==null){
            student.setId(UUID.randomUUID().toString());
        }
        students.put(student.getId(), student);
        return student;
    }

    public void deleteById(String id){
        students.remove(id);
    }

    public boolean existsById(String id){
        return students.containsKey(id);
    }

    public boolean existsByStudentId(String studentId){
        return students.values().stream()
                .anyMatch(s -> s.getStudentId().equals(studentId));
    }

    public boolean existsByStudentIdAndIdNot(String studentId, String id){
        return students.values().stream()
                .anyMatch(s -> s.getStudentId().equals(studentId) && !s.getId().equals(id));
    }


}


















