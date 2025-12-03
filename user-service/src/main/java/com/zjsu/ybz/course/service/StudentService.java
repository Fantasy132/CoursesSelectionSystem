package com.zjsu.ybz.course.service;

import com.zjsu.ybz.course.model.Student;
import com.zjsu.ybz.course.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class StudentService {
    @Autowired
    private StudentRepository studentRepository;

    // 删除 enrollmentRepository 注入

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+?@(.+)$");

    // 得到所有学生
    public List<Student> getAllStudents(){
        return studentRepository.findAll();
    }

    // 通过学生ID找到学生
    public Optional<Student> getStudentById(String id){
        return studentRepository.findById(id);
    }
    
    // 通过学号找到学生
    public Optional<Student> getStudentByStudentId(String studentId){
        return studentRepository.findByStudentId(studentId);
    }

    // 创建学生信息
    public Student createStudent(Student student){
        // 验证邮箱格式
        if(!EMAIL_PATTERN.matcher(student.getEmail()).matches()){
            throw new RuntimeException("Invalid email format");
        }

        // 检查studentId是否存在
        if(studentRepository.existsByStudentId(student.getStudentId())){
            throw new RuntimeException("Student ID already exists");
        }

        return studentRepository.save(student);
    }

    // 更新学生信息
    public Student updateStudent(String id, Student studentDetails){
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // 验证邮箱格式
        if(!EMAIL_PATTERN.matcher(studentDetails.getEmail()).matches()){
            throw new RuntimeException("Invalid email format");
        }

        // 检查studentId是否与其他学生重复
        if(studentRepository.existsByStudentIdAndIdNot(studentDetails.getStudentId(),id)){
            throw new RuntimeException("Student ID already exists");
        }

        student.setStudentId(studentDetails.getStudentId());
        student.setEmail(studentDetails.getEmail());
        student.setName(studentDetails.getName());
        student.setMajor(studentDetails.getMajor());
        student.setGrade(studentDetails.getGrade());

        return studentRepository.save(student);
    }

    // 删除学生信息（简化版，不检查选课记录）
    public void deleteStudent(String id){
        if(!studentRepository.existsById(id)){
            throw new RuntimeException("Student not found");
        }
        
        studentRepository.deleteById(id);
    }
}





















