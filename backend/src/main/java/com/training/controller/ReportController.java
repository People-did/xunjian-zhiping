package com.training.controller;

import com.training.common.Constants;
import com.training.common.PageResult;
import com.training.common.Result;
import com.training.config.SecurityConfig;
import com.training.dto.ReportDTO;
import com.training.entity.TrainingReport;
import com.training.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {
    
    private final ReportService reportService;
    
    @Value("${upload.path}")
    private String uploadPath;
    
    @GetMapping("/list")
    public Result<PageResult<ReportDTO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Integer status,
            @AuthenticationPrincipal SecurityConfig.UserPrincipal principal) {
        
        return Result.success(reportService.pageList(pageNum, pageSize, studentId, courseId, classId, status, 
                principal != null ? principal.getUserId() : null,
                principal != null ? principal.getRole() : null));
    }
    
    @GetMapping("/{id}")
    public Result<ReportDTO> getReport(@PathVariable Long id) {
        return reportService.getReport(id);
    }
    
    @PostMapping("/upload")
    public Result<Void> upload(
            @RequestParam Long studentId,
            @RequestParam Long courseId,
            @RequestParam String title,
            @RequestParam("file") MultipartFile file) {
        return reportService.uploadReport(studentId, courseId, title, file);
    }

    /**
     * 【全新】多文件批量上传 — 支持文档+截图+代码+ZIP混合上传
     */
    @PostMapping("/upload/multi")
    public Result<Void> uploadMultiple(
            @RequestParam Long studentId,
            @RequestParam Long courseId,
            @RequestParam String title,
            @RequestParam("files") List<MultipartFile> files) {
        return reportService.uploadMultipleFiles(studentId, courseId, title, files);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return reportService.deleteReport(id);
    }
    
    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        try {
            String filePath = reportService.getFilePath(id);
            if (filePath == null) {
                response.setStatus(404);
                return;
            }
            
            File file = new File(filePath);
            if (!file.exists()) {
                response.setStatus(404);
                return;
            }
            
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + 
                    URLEncoder.encode(file.getName(), StandardCharsets.UTF_8));
            
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            response.setStatus(500);
        }
    }

    /**
     * 获取学生成绩列表（学生查看自己的成绩）
     */
    @GetMapping("/my-scores")
    public Result<PageResult<ReportDTO>> getMyScores(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long courseId,
            @AuthenticationPrincipal SecurityConfig.UserPrincipal principal) {
        
        if (principal == null) {
            return Result.error("请先登录");
        }
        
        return Result.success(reportService.getMyScores(principal.getUserId(), pageNum, pageSize, courseId));
    }
}
