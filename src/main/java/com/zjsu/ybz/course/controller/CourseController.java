package com.zjsu.ybz.course.controller;

import com.zjsu.ybz.course.model.Course;
import com.zjsu.ybz.course.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    @Autowired
    private CourseService courseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Course>>> getAllCourses(){  // 获取所有课程
        List<Course> courses = courseService.getAllCourses();
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> getCourseById(@PathVariable String id){  // 通过课程ID得到课程信息
        return courseService.getCourseById(id)
                .map(course -> ResponseEntity.ok(ApiResponse.success(course)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(404, "Course not found")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Course>> createCourse(@RequestBody Course course){ // 创建课程信息
        Course created = courseService.createCourse(course);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> updateCourse( // 更新课程信息
            @PathVariable String id,
            @RequestBody Course course){
        try{
            Course updated = courseService.updateCourse(id, course);
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch(RuntimeException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable String id){ // 通过课程ID删除课程
        try{
            courseService.deleteCourse(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch(RuntimeException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

}























