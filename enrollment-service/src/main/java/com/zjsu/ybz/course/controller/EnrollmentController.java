package com.zjsu.ybz.course.controller;

import com.zjsu.ybz.course.model.Enrollment;
import com.zjsu.ybz.course.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    @Autowired
    private EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<Enrollment>> enroll(@RequestBody Map<String, String> request) { // 加入课程
        try {
            String courseId = request.get("courseId");
            String studentId = request.get("studentId");
            Enrollment created = enrollmentService.enroll(courseId, studentId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(created));
        } catch (RuntimeException e) {
            int status = e.getMessage().contains("not found") ? 404 : 400;
            return ResponseEntity.status(status)
                    .body(ApiResponse.error(status, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> withdraw(@PathVariable String id) {  // 撤销选课
        try {
            enrollmentService.withdraw(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Enrollment>>> getAllEnrollments() { // 获取所有选课信息
        List<Enrollment> enrollments = enrollmentService.getAllEnrollments();
        return ResponseEntity.ok(ApiResponse.success(enrollments));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<Enrollment>>> getEnrollmentsByCourse( // 通过课程ID获取选课信息
            @PathVariable String courseId) {
        List<Enrollment> enrollments = enrollmentService.getEnrollmentsByCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success(enrollments));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<Enrollment>>> getEnrollmentsByStudent( // 通过学生ID获取选课信息
            @PathVariable String studentId) {
        List<Enrollment> enrollments = enrollmentService.getEnrollmentsByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success(enrollments));
    }

}
