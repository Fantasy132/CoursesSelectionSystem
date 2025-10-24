package com.zjsu.ybz.course.controller;

import com.zjsu.ybz.course.model.Student;
import com.zjsu.ybz.course.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    @Autowired
    private StudentService studentService;

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

























