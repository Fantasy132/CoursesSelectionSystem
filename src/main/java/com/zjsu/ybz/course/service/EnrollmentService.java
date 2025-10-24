package com.zjsu.ybz.course.service;

import com.zjsu.ybz.course.model.Course;
import com.zjsu.ybz.course.model.Enrollment;
import com.zjsu.ybz.course.repository.EnrollmentRepository;
import com.zjsu.ybz.course.repository.CourseRepository;
import com.zjsu.ybz.course.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@Service
public class EnrollmentService {
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseService courseService;

    // 加入课程
    public Enrollment enroll(Enrollment enrollment){
        // 验证课程是否存在
        Course course = courseRepository.findById(enrollment.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // 验证学生是否存在
        if(!studentRepository.existsById(enrollment.getStudentId())){
            throw new RuntimeException("Student not found");
        }

        // 检查课程容量
        if(course.getEnrolled() >= course.getCapacity()){
            throw new RuntimeException("Course is full!");
        }

        // 检查重复选课
        if(enrollmentRepository.existsByCourseIdAndStudentId(
                enrollment.getCourseId(), enrollment.getStudentId()
        )){
            throw new RuntimeException("Already enrolled in this course");
        }

        // 保存选课记录
        Enrollment saved = enrollmentRepository.save(enrollment);

        // 增加课程的已选人数
        courseService.incrementEnrolled(enrollment.getCourseId());

        return saved;
    }

    // 撤回选课记录
    public void withdraw(String enrollmentId){
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        enrollmentRepository.deleteById(enrollmentId);

        // 减少课程的已选人数
        courseService.decrementEnrolled(enrollment.getCourseId());
    }

    // 获取所有选课记录
    public List<Enrollment> getAllEnrollments(){
        return enrollmentRepository.findAll();
    }

    // 选出指定课程ID的选课记录
    public List<Enrollment> getEnrollmentsByCourse(String courseId){
        return enrollmentRepository.findByCourseId(courseId);
    }

    // 选出指定学生ID的选课记录
    public List<Enrollment> getEnrollmentsByStudent(String studentId){
        return enrollmentRepository.findByStudentId(studentId);
    }
}






















