package com.zjsu.ybz.course.service;

import com.zjsu.ybz.course.model.Course;
import com.zjsu.ybz.course.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CourseService {
    @Autowired
    private CourseRepository courseRepository;

    // 获取所有课程信息
    public List<Course> getAllCourses(){
        return courseRepository.findAll();
    }

    // 获取指定ID的课程信息
    public Optional<Course> getCourseById(String id){
        return courseRepository.findById(id);
    }

    // 创建课程信息
    public Course createCourse(Course course){
        return courseRepository.save(course);
    }

    // 更新课程信息
    public Course updateCourse(String id, Course courseDetails){
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        course.setCode(courseDetails.getCode());
        course.setTitle(courseDetails.getTitle());
        course.setInstructor(courseDetails.getInstructor());
        course.setSchedule(courseDetails.getSchedule());
        course.setCapacity(courseDetails.getCapacity());

        return courseRepository.save(course);
    }

    // 删除课程信息
    public void deleteCourse(String id){
        if (!courseRepository.existsById(id)){
            throw new RuntimeException("Course not found");
        }
        courseRepository.deleteById(id);
    }

    // 课程选课人数+1
    public void incrementEnrolled(String courseId){
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setEnrolled(course.getEnrolled()+1);
        courseRepository.save(course);
    }

    // 课程选课人数-1
    public void decrementEnrolled(String courseId){
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setEnrolled(course.getEnrolled()-1);
        courseRepository.save(course);
    }
}






















