package com.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.training.common.Constants;
import com.training.common.PageResult;
import com.training.common.Result;
import com.training.dto.ReportDTO;
import com.training.entity.Course;
import com.training.entity.EvaluationRecord;
import com.training.entity.SysClass;
import com.training.entity.SysUser;
import com.training.entity.TrainingReport;
import com.training.mapper.EvaluationRecordMapper;
import com.training.mapper.CourseMapper;
import com.training.mapper.SysClassMapper;
import com.training.mapper.SysUserMapper;
import com.training.mapper.TrainingReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService extends ServiceImpl<TrainingReportMapper, TrainingReport> {
    
    private final TrainingReportMapper reportMapper;
    private final SysUserMapper userMapper;
    private final SysClassMapper classMapper;
    private final EvaluationRecordMapper evaluationMapper;
    private final CourseMapper courseMapper;
    
    @Value("${upload.path}")
    private String uploadPath;
    
    public PageResult<ReportDTO> pageList(Integer pageNum, Integer pageSize, Long studentId, 
                                         Long courseId, Long classId, Integer status, Long currentUserId, Integer currentRole) {
        Page<TrainingReport> page = new Page<>(pageNum, pageSize);
        
        if (currentRole != null && currentRole == Constants.ROLE_STUDENT) {
            studentId = currentUserId;
        }
        
        IPage<ReportDTO> result = reportMapper.selectReportPage(page, studentId, courseId, classId, status);
        
        return new PageResult<>(result.getTotal(), result.getRecords());
    }
    
    public Result<ReportDTO> getReport(Long id) {
        TrainingReport report = reportMapper.selectById(id);
        if (report == null) {
            return Result.error("报告不存在");
        }
        
        return Result.success(convertToDTO(report));
    }
    
    public Result<Void> uploadReport(Long studentId, Long courseId, String title, MultipartFile file) {
        try {
            if (courseId == null) {
                return Result.error("请选择课程");
            }
            
            String originalFilename = file.getOriginalFilename();
            String extension = com.training.utils.FileUtils.getFileExtension(originalFilename);
            
            if (!com.training.utils.FileUtils.isAllowedFileType(extension)) {
                return Result.error("不支持的文件类型，仅支持doc、docx、pdf");
            }
            
            String filename = com.training.utils.FileUtils.generateUniqueFilename(originalFilename);
            
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            File destFile = new File(uploadDir, filename);
            file.transferTo(destFile);
            
            String content = com.training.utils.FileUtils.extractTextFromFile(file);
            if (content != null && content.length() > 1000) {
                content = content.substring(0, 1000);
            }
            
            TrainingReport report = new TrainingReport();
            report.setStudentId(studentId);
            report.setCourseId(courseId);
            report.setTitle(title);
            report.setContent(content);
            report.setFilePath(destFile.getAbsolutePath());
            report.setFileName(originalFilename);
            report.setFileType(extension);
            report.setStatus(Constants.REPORT_PENDING);
            report.setCreateTime(LocalDateTime.now());
            
            reportMapper.insert(report);

            return Result.success();
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 【全新】多文件批量上传 — 支持文档+截图+代码+ZIP混合上传
     * 将多个文件合并为一个报告记录，content 字段汇总所有提取的文本
     */
    public Result<Void> uploadMultipleFiles(Long studentId, Long courseId, String title, List<MultipartFile> files) {
        try {
            if (courseId == null) {
                return Result.error("请选择课程");
            }
            if (files == null || files.isEmpty()) {
                return Result.error("请至少上传一个文件");
            }

            StringBuilder allContent = new StringBuilder();
            allContent.append("【多文件上传成果】\n标题: ").append(title).append("\n\n");
            allContent.append("上传文件清单:\n");

            List<String> fileNames = new ArrayList<>();
            List<String> fileTypes = new ArrayList<>();
            String primaryFilePath = null;
            String primaryFileName = null;
            String primaryFileType = null;

            int idx = 0;
            for (MultipartFile file : files) {
                idx++;
                String originalFilename = file.getOriginalFilename();
                String extension = com.training.utils.FileUtils.getFileExtension(originalFilename);

                if (!com.training.utils.FileUtils.isAllowedFileType(extension)) {
                    return Result.error("不支持的文件类型: " + extension + "（文件: " + originalFilename + "）");
                }

                String filename = com.training.utils.FileUtils.generateUniqueFilename(originalFilename);

                // 保存文件
                File uploadDir = new File(uploadPath);
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                File destFile = new File(uploadDir, filename);
                file.transferTo(destFile);

                // 提取文本内容
                allContent.append("\n--- 文件").append(idx).append(": ").append(originalFilename).append(" ---\n");
                allContent.append("类型: ").append(com.training.utils.FileUtils.getFileCategoryLabel(extension)).append("\n");

                try {
                    String extractedText = com.training.utils.FileUtils.extractTextFromFile(file);
                    if (extractedText != null && extractedText.length() > 2000) {
                        extractedText = extractedText.substring(0, 2000) + "\n... (已截断)";
                    }
                    allContent.append("内容:\n").append(extractedText).append("\n");
                } catch (Exception e) {
                    allContent.append("[无法提取文本: ").append(e.getMessage()).append("]\n");
                }

                fileNames.add(originalFilename);
                fileTypes.add(extension);

                // 第一个文件作为主文件
                if (idx == 1) {
                    primaryFilePath = destFile.getAbsolutePath();
                    primaryFileName = originalFilename;
                    primaryFileType = extension;
                }
            }

            // 创建报告记录（以第一个文件为主文件，content 汇总所有文本）
            TrainingReport report = new TrainingReport();
            report.setStudentId(studentId);
            report.setCourseId(courseId);
            report.setTitle(title);
            report.setContent(allContent.toString());
            report.setFilePath(primaryFilePath);
            report.setFileName(String.join("; ", fileNames)); // 多个文件名用分号连接
            report.setFileType(String.join(";", fileTypes));   // 多个文件类型
            report.setStatus(Constants.REPORT_PENDING);
            report.setCreateTime(LocalDateTime.now());

            reportMapper.insert(report);

            return Result.success();
        } catch (Exception e) {
            return Result.error("批量上传失败: " + e.getMessage());
        }
    }

    public Result<Void> deleteReport(Long id) {
        TrainingReport report = reportMapper.selectById(id);
        if (report == null) {
            return Result.error("报告不存在");
        }
        
        if (report.getFilePath() != null) {
            new File(report.getFilePath()).delete();
        }
        
        LambdaQueryWrapper<EvaluationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EvaluationRecord::getReportId, id);
        evaluationMapper.delete(wrapper);
        
        reportMapper.deleteById(id);
        
        return Result.success();
    }
    
    public String getFilePath(Long id) {
        TrainingReport report = reportMapper.selectById(id);
        return report != null ? report.getFilePath() : null;
    }
    
    public void updateStatus(Long reportId, Integer status) {
        TrainingReport report = reportMapper.selectById(reportId);
        if (report != null) {
            report.setStatus(status);
            report.setUpdateTime(LocalDateTime.now());
            reportMapper.updateById(report);
        }
    }
    
    public List<Map<String, Object>> getStudentScores(Long studentId) {
        return reportMapper.selectScoreByClassId(studentId);
    }

    public PageResult<ReportDTO> getMyScores(Long studentId, Integer pageNum, Integer pageSize, Long courseId) {
        Page<TrainingReport> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<TrainingReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingReport::getStudentId, studentId);
        if (courseId != null) {
            wrapper.eq(TrainingReport::getCourseId, courseId);
        }
        wrapper.orderByDesc(TrainingReport::getCreateTime);
        
        Page<TrainingReport> result = reportMapper.selectPage(page, wrapper);
        
        List<ReportDTO> dtoList = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return new PageResult<>(result.getTotal(), dtoList);
    }
    
    private ReportDTO convertToDTO(TrainingReport report) {
        ReportDTO dto = new ReportDTO();
        dto.setId(report.getId());
        dto.setStudentId(report.getStudentId());
        dto.setCourseId(report.getCourseId());
        dto.setTitle(report.getTitle());
        dto.setContent(report.getContent());
        dto.setFilePath(report.getFilePath());
        dto.setFileName(report.getFileName());
        dto.setFileType(report.getFileType());
        dto.setStatus(report.getStatus());
        dto.setStatusName(Constants.getReportStatusName(report.getStatus()));
        dto.setCreateTime(report.getCreateTime());
        dto.setUpdateTime(report.getUpdateTime());
        
        SysUser student = userMapper.selectById(report.getStudentId());
        if (student != null) {
            dto.setStudentName(student.getRealName());
            
            if (student.getClassId() != null) {
                SysClass sysClass = classMapper.selectById(student.getClassId());
                if (sysClass != null) {
                    dto.setClassName(sysClass.getClassName());
                }
            }
        }
        
        if (report.getCourseId() != null) {
            com.training.entity.Course course = courseMapper.selectById(report.getCourseId());
            if (course != null) {
                dto.setCourseName(course.getCourseName());
            }
        }
        
        LambdaQueryWrapper<EvaluationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EvaluationRecord::getReportId, report.getId());
        EvaluationRecord evaluation = evaluationMapper.selectOne(wrapper);
        if (evaluation != null) {
            dto.setEvaluationId(evaluation.getId());
            dto.setCompletenessScore(evaluation.getCompletenessScore());
            dto.setSpecificationScore(evaluation.getSpecificationScore());
            dto.setKnowledgeScore(evaluation.getKnowledgeScore());
            dto.setTotalScore(evaluation.getTotalScore());
            dto.setAiEvaluation(evaluation.getAiEvaluation());
            dto.setManualEvaluation(evaluation.getManualEvaluation());
            dto.setDynamicScoresJson(evaluation.getDynamicScoresJson()); // 织入对动态JSON分值的支持
            dto.setEvaluateTime(evaluation.getEvaluateTime());
        }
        
        return dto;
    }
}