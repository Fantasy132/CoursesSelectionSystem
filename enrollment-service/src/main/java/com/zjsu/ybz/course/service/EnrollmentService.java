package com.zjsu.ybz.course.service;

import com.zjsu.ybz.course.client.CatalogClient;
import com.zjsu.ybz.course.client.UserClient;
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
import java.time.LocalDateTime;
import java.util.*;

@Service
public class EnrollmentService {
    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private UserClient userClient;
    
    @Autowired
    private CatalogClient catalogClient;

    @Transactional
    public Enrollment enroll(String courseId, String studentId) {
        log.info("开始处理选课请求: courseId={}, studentId={}", courseId, studentId);
        
        // 1. 调用 user-service 验证学生是否存在 (使用 Feign Client)
        Map<String, Object> studentResponse;
        try {
            log.debug("调用用户服务验证学生: studentId={}", studentId);
            studentResponse = userClient.getStudentByStudentId(studentId);
            
            // 检查是否为降级响应
            Boolean success = (Boolean) studentResponse.get("success");
            if (success != null && !success) {
                String message = (String) studentResponse.get("message");
                log.error("用户服务降级: {}", message);
                throw new BusinessException(message);
            }
            
            if (studentResponse.get("data") == null) {
                log.warn("学生不存在: studentId={}", studentId);
                throw new ResourceNotFoundException("Student", studentId);
            }
        } catch (feign.FeignException.NotFound e) {
            log.warn("学生不存在 (404): studentId={}", studentId);
            throw new ResourceNotFoundException("Student", studentId);
        } catch (feign.FeignException e) {
            log.error("调用用户服务失败: studentId={}, error={}", studentId, e.getMessage());
            throw new BusinessException("Failed to verify student: " + e.getMessage());
        }
        
        log.debug("学生验证通过: studentId={}", studentId);

        // 2. 调用 catalog-service 验证课程是否存在 (使用 Feign Client)
        Map<String, Object> courseResponse;
        try {
            log.debug("调用课程目录服务: courseId={}", courseId);
            courseResponse = catalogClient.getCourseById(courseId);
            
            // 检查是否为降级响应
            Boolean success = (Boolean) courseResponse.get("success");
            if (success != null && !success) {
                String message = (String) courseResponse.get("message");
                log.error("课程目录服务降级: {}", message);
                throw new BusinessException(message);
            }
            
            if (courseResponse.get("data") == null) {
                log.warn("课程不存在: courseId={}", courseId);
                throw new ResourceNotFoundException("Course", courseId);
            }
        } catch (feign.FeignException.NotFound e) {
            log.warn("课程不存在 (404): courseId={}", courseId);
            throw new ResourceNotFoundException("Course", courseId);
        } catch (feign.FeignException e) {
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
        
        // 获取课程当前已选人数 (使用 Feign Client)
        try {
            log.debug("调用课程目录服务: courseId={}", courseId);
            Map<String, Object> courseResponse = catalogClient.getCourseById(courseId);
            
            // 检查是否为降级响应
            Boolean success = (Boolean) courseResponse.get("success");
            if (success != null && !success) {
                String message = (String) courseResponse.get("message");
                log.error("课程目录服务降级: {}", message);
                throw new BusinessException(message);
            }
            
            Map<String, Object> courseData = (Map<String, Object>) courseResponse.get("data");
            Integer enrolled = (Integer) courseData.get("enrolled");
            
            log.debug("课程当前已选人数: enrolled={}", enrolled);
            
            // 删除选课记录
            enrollmentRepository.deleteById(enrollmentId);
            log.info("选课记录已删除: enrollmentId={}", enrollmentId);
            
            // 减少课程的已选人数
            updateCourseEnrolledCount(courseId, enrolled - 1);
            
            log.info("退课成功: enrollmentId={}, courseId={}, studentId={}", enrollmentId, courseId, studentId);
        } catch (feign.FeignException.NotFound e) {
            log.warn("课程不存在 (404): courseId={}", courseId);
            throw new ResourceNotFoundException("Course", courseId);
        } catch (feign.FeignException e) {
            log.error("退课失败: enrollmentId={}, error={}", enrollmentId, e.getMessage());
            throw new BusinessException("Failed to withdraw from course: " + e.getMessage());
        }
    }
    
    private void updateCourseEnrolledCount(String courseId, int newCount) {
        Map<String, Integer> request = new HashMap<>();
        request.put("enrolled", newCount);
        
        try {
            catalogClient.updateCourseEnrolledCount(courseId, request);
            log.info("课程已选人数已更新: courseId={}, newCount={}", courseId, newCount);
        } catch (feign.FeignException.NotFound e) {
            log.error("课程不存在 (404): courseId={}", courseId);
            throw new ResourceNotFoundException("Course", courseId);
        } catch (feign.FeignException e) {
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