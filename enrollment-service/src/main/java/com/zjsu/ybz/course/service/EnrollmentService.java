package com.zjsu.ybz.course.service;

import com.zjsu.ybz.course.exception.BusinessException;
import com.zjsu.ybz.course.exception.ResourceNotFoundException;
import com.zjsu.ybz.course.model.Enrollment;
import com.zjsu.ybz.course.model.EnrollmentStatus;
import com.zjsu.ybz.course.repository.EnrollmentRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class EnrollmentService {
    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);
    
    // 服务名称常量
    private static final String USER_SERVICE = "user-service";
    private static final String CATALOG_SERVICE = "catalog-service";
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private RestTemplate restTemplate;

    @Transactional
    public Enrollment enroll(String courseId, String studentId) {
        log.info("开始处理选课请求: courseId={}, studentId={}", courseId, studentId);
        
        // 1. 调用 user-service 验证学生是否存在 (使用服务名)
        String userUrl = "http://" + USER_SERVICE + "/api/students/studentId/" + studentId;
        Map<String, Object> studentResponse;
        try {
            log.debug("调用用户服务验证学生: url={}", userUrl);
            studentResponse = restTemplate.getForObject(userUrl, Map.class);
            if (studentResponse == null || studentResponse.get("data") == null) {
                log.warn("学生不存在: studentId={}", studentId);
                throw new ResourceNotFoundException("Student", studentId);
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("学生不存在 (404): studentId={}", studentId);
            throw new ResourceNotFoundException("Student", studentId);
        } catch (ResourceAccessException e) {
            log.error("调用用户服务失败 (网络错误): studentId={}, error={}", studentId, e.getMessage());
            throw new BusinessException("User service is unavailable: " + e.getMessage());
        } catch (Exception e) {
            log.error("调用用户服务失败: studentId={}, error={}", studentId, e.getMessage());
            throw new BusinessException("Failed to verify student: " + e.getMessage());
        }
        
        log.debug("学生验证通过: studentId={}", studentId);

        // 2. 调用 catalog-service 验证课程是否存在 (使用服务名)
        String courseUrl = "http://" + CATALOG_SERVICE + "/api/courses/" + courseId;
        Map<String, Object> courseResponse;
        try {
            log.debug("调用课程目录服务: url={}", courseUrl);
            courseResponse = restTemplate.getForObject(courseUrl, Map.class);
            if (courseResponse == null || courseResponse.get("data") == null) {
                log.warn("课程不存在: courseId={}", courseId);
                throw new ResourceNotFoundException("Course", courseId);
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("课程不存在 (404): courseId={}", courseId);
            throw new ResourceNotFoundException("Course", courseId);
        } catch (ResourceAccessException e) {
            log.error("调用课程目录服务失败 (网络错误): courseId={}, error={}", courseId, e.getMessage());
            throw new BusinessException("Catalog service is unavailable: " + e.getMessage());
        } catch (Exception e) {
            log.error("调用课程目录服务失败: courseId={}, error={}", courseId, e.getMessage());
            throw new BusinessException("Failed to verify course: " + e.getMessage());
        }

        // 3. 从响应中提取课程信息
        Map<String, Object> courseData = (Map<String, Object>) courseResponse.get("data");
        Integer capacity = (Integer) courseData.get("capacity");
        Integer enrolled = (Integer) courseData.get("enrolled");
        
        log.debug("课程信息: capacity={}, enrolled={}", capacity, enrolled);

        // 4. 检查课程容量
        if (enrolled >= capacity) {
            log.warn("课程已满: courseId={}, capacity={}, enrolled={}", courseId, capacity, enrolled);
            throw new BusinessException("Course is full");
        }

        // 5. 检查是否重复选课
        if (enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            log.warn("重复选课: courseId={}, studentId={}", courseId, studentId);
            throw new BusinessException("Student already enrolled in this course");
        }

        // 6. 创建选课记录
        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(courseId);
        enrollment.setStudentId(studentId);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrolledAt(LocalDateTime.now());

        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("选课成功: enrollmentId={}, courseId={}, studentId={}", saved.getId(), courseId, studentId);

        // 7. 更新课程的已选人数（调用catalog-service）
        updateCourseEnrolledCount(courseId, enrolled + 1);

        return saved;
    }

    @Transactional
    public void withdraw(String enrollmentId) {
        log.info("开始处理退课请求: enrollmentId={}", enrollmentId);
        
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> {
                    log.warn("选课记录不存在: enrollmentId={}", enrollmentId);
                    return new ResourceNotFoundException("Enrollment", enrollmentId);
                });

        String courseId = enrollment.getCourseId();
        String studentId = enrollment.getStudentId();
        log.debug("退课信息: courseId={}, studentId={}", courseId, studentId);
        
        // 获取课程当前已选人数 (使用服务名)
        String url = "http://" + CATALOG_SERVICE + "/api/courses/" + courseId;
        try {
            log.debug("调用课程目录服务: url={}", url);
            Map<String, Object> courseResponse = restTemplate.getForObject(url, Map.class);
            Map<String, Object> courseData = (Map<String, Object>) courseResponse.get("data");
            Integer enrolled = (Integer) courseData.get("enrolled");
            
            log.debug("课程当前已选人数: enrolled={}", enrolled);
            
            // 删除选课记录
            enrollmentRepository.deleteById(enrollmentId);
            log.info("选课记录已删除: enrollmentId={}", enrollmentId);
            
            // 减少课程的已选人数
            updateCourseEnrolledCount(courseId, enrolled - 1);
            
            log.info("退课成功: enrollmentId={}, courseId={}, studentId={}", enrollmentId, courseId, studentId);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("课程不存在 (404): courseId={}", courseId);
            throw new ResourceNotFoundException("Course", courseId);
        } catch (ResourceAccessException e) {
            log.error("调用课程目录服务失败 (网络错误): courseId={}, error={}", courseId, e.getMessage());
            throw new BusinessException("Catalog service is unavailable: " + e.getMessage());
        } catch (Exception e) {
            log.error("退课失败: enrollmentId={}, error={}", enrollmentId, e.getMessage(), e);
            throw new BusinessException("Failed to withdraw from course: " + e.getMessage());
        }
    }
    
    private void updateCourseEnrolledCount(String courseId, int newCount) {
        // 使用服务名调用 catalog-service
        String url = "http://" + CATALOG_SERVICE + "/api/courses/" + courseId + "/update-enrolled";
        Map<String, Integer> request = new HashMap<>();
        request.put("enrolled", newCount);
        
        try {
            restTemplate.put(url, request);
            log.info("课程已选人数已更新: courseId={}, newCount={}", courseId, newCount);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("课程不存在 (404): courseId={}", courseId);
            throw new ResourceNotFoundException("Course", courseId);
        } catch (ResourceAccessException e) {
            log.error("调用课程目录服务失败 (网络错误): courseId={}, error={}", courseId, e.getMessage());
            throw new BusinessException("Catalog service is unavailable: " + e.getMessage());
        } catch (Exception e) {
            log.error("更新课程已选人数失败: courseId={}, error={}", courseId, e.getMessage());
            throw new BusinessException("Failed to update course enrolled count: " + e.getMessage());
        }
    }

    public List<Enrollment> getAllEnrollments(){
        log.debug("获取所有选课记录");
        return enrollmentRepository.findAll();
    }

    public List<Enrollment> getEnrollmentsByCourse(String courseId){
        log.debug("获取课程的选课记录: courseId={}", courseId);
        return enrollmentRepository.findByCourseId(courseId);
    }

    public List<Enrollment> getEnrollmentsByStudent(String studentId){
        log.debug("获取学生的选课记录: studentId={}", studentId);
        return enrollmentRepository.findByStudentId(studentId);
    }
}