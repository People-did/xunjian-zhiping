package com.training.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.common.Constants;
import com.training.common.PageResult;
import com.training.common.Result;
import com.training.config.SecurityConfig;
import com.training.dto.ReportDTO;
import com.training.dto.StatisticsDTO;
import com.training.dto.UserDTO;
import com.training.entity.SysClass;
import com.training.service.ClassService;
import com.training.service.ReportService;
import com.training.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/class")
@RequiredArgsConstructor
public class ClassController {
    
    private final ClassService classService;
    private final ReportService reportService;
    private final StatisticsService statisticsService;
    
    @GetMapping("/list")
    public Result<PageResult<SysClass>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        
        return Result.success(classService.pageList(pageNum, pageSize, keyword));
    }
    
    @GetMapping("/all")
    public Result<List<SysClass>> listAll() {
        return Result.success(classService.listAll());
    }
    
    /**
     * 获取教师所教的班级列表
     */
    @GetMapping("/teacher")
    public Result<List<Map<String, Object>>> getTeacherClasses(
            @AuthenticationPrincipal SecurityConfig.UserPrincipal principal) {
        if (principal == null || principal.getRole() != Constants.ROLE_TEACHER) {
            return Result.error("无权限访问");
        }
        return Result.success(classService.getTeacherClasses(principal.getUserId()));
    }
    
    /**
     * 获取班级详情（含统计信息）
     */
    @GetMapping("/{id}/detail")
    public Result<Map<String, Object>> getClassDetail(@PathVariable Long id) {
        return Result.success(classService.getClassDetail(id));
    }
    
    /**
     * 获取班级的报告列表
     */
    @GetMapping("/{id}/reports")
    public Result<PageResult<ReportDTO>> getClassReports(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long courseId) {
        return Result.success(reportService.pageList(pageNum, pageSize, null, courseId, id, null, null, null));
    }
    
    /**
     * 获取班级统计信息
     */
    @GetMapping("/{id}/statistics")
    public Result<StatisticsDTO> getClassStatistics(@PathVariable Long id) {
        return statisticsService.getClassStatistics(id);
    }
    
    @GetMapping("/{id}/students")
    public Result<List<UserDTO>> getClassStudents(@PathVariable Long id) {
        return Result.success(classService.getClassStudents(id));
    }
    
    @PostMapping
    public Result<Void> add(@RequestBody SysClass sysClass) {
        return classService.addClass(sysClass);
    }
    
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysClass sysClass) {
        sysClass.setId(id);
        return classService.updateClass(sysClass);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return classService.deleteClass(id);
    }
}
