package com.zjsu.ybz.course.controller;

import com.zjsu.ybz.course.model.Student;
import com.zjsu.ybz.course.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private static final Logger log = LoggerFactory.getLogger(StudentController.class);
    
    @Autowired
    private StudentService studentService;
    
    @Value("${server.port}")
    private String serverPort;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Student>>> getAllStudents(){ // 获取所有学生信息
        List<Student> students = studentService.getAllStudents();
        return ResponseEntity.ok(ApiResponse.success(students));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Student>> getStudentById(@PathVariable String id){ // 通过学生ID获取学生信息
        return studentService.getStudentById(id)
                .map(student -> ResponseEntity.ok(ApiResponse.success(student)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(404, "Student not found")));
    }

    @GetMapping("/studentId/{studentId}")
    public ResponseEntity<ApiResponse<Student>> getStudentByStudentId(@PathVariable String studentId){ // 通过学号获取学生信息
        log.info("【实例 {}】收到请求: GET /api/students/studentId/{}", serverPort, studentId);
        return studentService.getStudentByStudentId(studentId)
                .map(student -> ResponseEntity.ok(ApiResponse.success(student)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(404, "Student not found")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Student>> createStudent(@RequestBody Student student){ // 创建学生信息
        try{
            Student created = studentService.createStudent(student);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(created));
        } catch(RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Student>> updateStudent( // 更新学生信息
            @PathVariable String id, @RequestBody Student student) {
        try {
            Student updated = studentService.updateStudent(id, student);
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (RuntimeException e) {
            int status = e.getMessage().contains("not found") ? 404 : 400;
            return ResponseEntity.status(status)
                    .body(ApiResponse.error(status, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable String id) { // 删除学生信息
        try {
            studentService.deleteStudent(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (RuntimeException e) {
            int status = e.getMessage().contains("not found") ? 404 : 400;
            return ResponseEntity.status(status)
                    .body(ApiResponse.error(status, e.getMessage()));
        }
    }
}

























