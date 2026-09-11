package com.training.controller;

import com.training.common.Constants;
import com.training.common.PageResult;
import com.training.common.Result;
import com.training.config.SecurityConfig;
import com.training.dto.CourseDTO;
import com.training.entity.Course;
import com.training.entity.SysUser;
import com.training.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseController {
    
    private final CourseService courseService;
    
    @GetMapping("/list")
    public Result<PageResult<CourseDTO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String courseName,
            @AuthenticationPrincipal SecurityConfig.UserPrincipal principal) {
        
        return Result.success(courseService.pageList(pageNum, pageSize, null, classId, courseName,
                principal != null ? principal.getUserId() : null,
                principal != null ? principal.getRole() : null));
    }
    
    @GetMapping("/{id}")
    public Result<CourseDTO> getCourse(@PathVariable Long id) {
        return courseService.getCourse(id);
    }
    
    @PostMapping
    public Result<Void> addCourse(@RequestBody Course course) {
        return courseService.addCourse(course);
    }
    
    @PutMapping
    public Result<Void> updateCourse(@RequestBody Course course) {
        return courseService.updateCourse(course);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        return courseService.deleteCourse(id);
    }
    
    /**
     * 获取学生可选的课程列表
     */
    @GetMapping("/available")
    public Result<List<CourseDTO>> getAvailableCourses(@AuthenticationPrincipal SecurityConfig.UserPrincipal principal) {
        if (principal == null) {
            return Result.error("请先登录");
        }
        return courseService.getAvailableCourses(principal.getUserId());
    }
    
    /**
     * 学生选课
     */
    @PostMapping("/select/{courseId}")
    public Result<Void> selectCourse(@PathVariable Long courseId,
                                     @AuthenticationPrincipal SecurityConfig.UserPrincipal principal) {
        if (principal == null) {
            return Result.error("请先登录");
        }
        return courseService.selectCourse(principal.getUserId(), courseId);
    }
    
    /**
     * 学生取消选课
     */
    @DeleteMapping("/cancel/{courseId}")
    public Result<Void> cancelCourse(@PathVariable Long courseId,
                                      @AuthenticationPrincipal SecurityConfig.UserPrincipal principal) {
        if (principal == null) {
            return Result.error("请先登录");
        }
        return courseService.cancelCourse(principal.getUserId(), courseId);
    }
    
    /**
     * 获取我的课程列表（根据角色返回不同数据）
     * 学生：返回已选的课程
     * 教师：返回被分配的课程
     */
    @GetMapping("/my-courses")
    public Result<List<Course>> getMyCourses(@AuthenticationPrincipal SecurityConfig.UserPrincipal principal) {
        if (principal == null) {
            return Result.error("请先登录");
        }
        // 教师角色返回被分配的课程
        if (principal.getRole() != null && principal.getRole() == 2) {
            return courseService.getTeacherCourses(principal.getUserId());
        }
        // 学生角色返回已选的课程
        return courseService.getStudentCourses(principal.getUserId());
    }
    
    /**
     * 获取课程下的学生列表
     */
    @GetMapping("/{courseId}/students")
    public Result<List<SysUser>> getCourseStudents(@PathVariable Long courseId) {
        return courseService.getCourseStudents(courseId);
    }
}
