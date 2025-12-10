package com.zjsu.ybz.course.controller;

import com.zjsu.ybz.course.model.Course;
import com.zjsu.ybz.course.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private static final Logger log = LoggerFactory.getLogger(CourseController.class);
    
    @Autowired
    private CourseService courseService;
    
    @Value("${server.port}")
    private String serverPort;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Course>>> getAllCourses(){  // 获取所有课程
        List<Course> courses = courseService.getAllCourses();
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> getCourseById(@PathVariable String id){  // 通过课程ID得到课程信息
        log.info("【实例 {}】收到请求: GET /api/courses/{}", serverPort, id);
        return courseService.getCourseById(id)
                .map(course -> ResponseEntity.ok(ApiResponse.success(course)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(404, "Course not found")));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<Course>> getCourseByCode(@PathVariable String code){  // 通过课程代码得到课程信息
        return courseService.getCourseByCode(code)
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
        log.info("【实例 {}】收到请求: PUT /api/courses/{}", serverPort, id);
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

    @PutMapping("/{id}/update-enrolled")
    public ResponseEntity<ApiResponse<Void>> updateEnrolled( // 更新课程选课人数
            @PathVariable String id,
            @RequestBody java.util.Map<String, Integer> request){
        try{
            Integer enrolled = request.get("enrolled");
            if(enrolled == null){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(400, "Enrolled count is required"));
            }
            courseService.updateEnrolled(id, enrolled);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch(RuntimeException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

}























