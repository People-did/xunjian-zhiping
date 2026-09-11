package com.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.training.common.Constants;
import com.training.common.PageResult;
import com.training.common.Result;
import com.training.dto.EvaluationDTO;
import com.training.entity.Course;
import com.training.entity.EvaluationRecord;
import com.training.entity.SysClass;
import com.training.entity.SysUser;
import com.training.entity.TrainingReport;
import com.training.entity.ReportRequirement;
import com.training.entity.EvaluationCriterion;
import com.training.mapper.CourseMapper;
import com.training.mapper.EvaluationRecordMapper;
import com.training.mapper.SysClassMapper;
import com.training.mapper.SysUserMapper;
import com.training.mapper.TrainingReportMapper;
import com.training.mapper.ReportRequirementMapper;
import com.training.mapper.EvaluationCriterionMapper;
import com.training.utils.AiUtils;
import com.training.utils.FileUtils;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService extends ServiceImpl<EvaluationRecordMapper, EvaluationRecord> {
    
    private final EvaluationRecordMapper evaluationMapper;
    private final TrainingReportMapper reportMapper;
    private final SysUserMapper userMapper;
    private final SysClassMapper classMapper;
    private final CourseMapper courseMapper;
    private final ReportService reportService;
    private final AiUtils aiUtils;
    
    // 注入必要的关联老表持久层，无损扩容
    private final ReportRequirementMapper requirementMapper;
    private final EvaluationCriterionMapper criterionMapper;
    
    public PageResult<EvaluationDTO> pageList(Integer pageNum, Integer pageSize, Long reportId, 
                                               Long teacherId, Long classId, Long courseId,
                                               Integer currentRole, Long currentUserId) {
        Page<EvaluationRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EvaluationRecord> wrapper = new LambdaQueryWrapper<>();
        
        if (reportId != null) {
            wrapper.eq(EvaluationRecord::getReportId, reportId);
        }
        if (teacherId != null) {
            wrapper.eq(EvaluationRecord::getTeacherId, teacherId);
        }
        
        if (currentRole != null && currentRole == Constants.ROLE_TEACHER) {
            LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
            courseWrapper.eq(Course::getTeacherId, currentUserId);
            List<Course> teacherCourses = courseMapper.selectList(courseWrapper);
            List<Long> courseIds = teacherCourses.stream().map(Course::getId).toList();
            
            if (!courseIds.isEmpty()) {
                LambdaQueryWrapper<TrainingReport> reportWrapper = new LambdaQueryWrapper<>();
                reportWrapper.in(TrainingReport::getCourseId, courseIds);
                List<TrainingReport> reports = reportMapper.selectList(reportWrapper);
                List<Long> reportIds = reports.stream().map(TrainingReport::getId).toList();
                
                if (!reportIds.isEmpty()) {
                    wrapper.in(EvaluationRecord::getReportId, reportIds);
                } else {
                    wrapper.eq(EvaluationRecord::getId, -1L);
                }
            } else {
                wrapper.eq(EvaluationRecord::getId, -1L);
            }
        }
        
        wrapper.orderByDesc(EvaluationRecord::getEvaluateTime);
        
        Page<EvaluationRecord> result = evaluationMapper.selectPage(page, wrapper);
        
        List<EvaluationDTO> dtoList = result.getRecords().stream()
                .map(this::convertToDTO)
                .toList();
        
        return new PageResult<>(result.getTotal(), dtoList);
    }
    
    /**
     * 【重构升级】AI智能评价核心算法（支持在宿主层自动分流处理传统三大指标与新扩容自定义指标）
     */
    @Transactional
    public Result<EvaluationDTO> aiEvaluate(Long reportId, Long teacherId) {
        TrainingReport report = reportMapper.selectById(reportId);
        if (report == null) {
            return Result.error("报告不存在");
        }
        
        LambdaQueryWrapper<EvaluationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EvaluationRecord::getReportId, reportId);
        EvaluationRecord existing = evaluationMapper.selectOne(wrapper);
        
        try {
            // 1. 业务逻辑溯源：反查这份实训报告所对应的发布作业要求
            ReportRequirement requirement = null;
            // 假设你的 training_report 表中有字段标记了它对应哪一条报告要求，若通过标题或课程反查：
            // 此处采用最安全的兼容查找：基于课程ID获取当前进行中的最新作业要求进行匹配
            LambdaQueryWrapper<ReportRequirement> reqWrapper = new LambdaQueryWrapper<>();
            reqWrapper.eq(ReportRequirement::getCourseId, report.getCourseId());
            reqWrapper.orderByDesc(ReportRequirement::getCreateTime);
            List<ReportRequirement> requirements = requirementMapper.selectList(reqWrapper);
            if (!requirements.isEmpty()) {
                requirement = requirements.get(0);
            }

            EvaluationRecord evaluation = (existing != null) ? existing : new EvaluationRecord();
            evaluation.setReportId(reportId);
            evaluation.setTeacherId(teacherId);
            evaluation.setIsAi(Constants.EVALUATION_AI);
            evaluation.setEvaluateTime(LocalDateTime.now());
            evaluation.setUpdateTime(LocalDateTime.now());

            // 2. 智能化分流决策树判定
            if (requirement != null && requirement.getHasCustomCriterion() == 1) {
                // 【自定义标准路由】
                // 级联查询出当前作业关联的所有动态细则条目集合
                LambdaQueryWrapper<EvaluationCriterion> criterionWrapper = new LambdaQueryWrapper<>();
                criterionWrapper.eq(EvaluationCriterion::getRequirementId, requirement.getId());
                List<EvaluationCriterion> criteria = criterionMapper.selectList(criterionWrapper);

                if (!criteria.isEmpty()) {
                    // 调用升级后的动态 Prompt 大模型拼接机制
                    String dynamicJsonResult = aiUtils.evaluateWithCustomCriteria(report.getContent(), criteria);
                    
                    // 解析出总分，以便更新在最外层用于班级报表统计（Sp_class_statistics存储过程兼容）
                    com.alibaba.fastjson2.JSONObject jsonObject = com.alibaba.fastjson2.JSON.parseObject(dynamicJsonResult);
                    BigDecimal calculatedTotal = jsonObject.getBigDecimal("totalScore");
                    
                    evaluation.setTotalScore(calculatedTotal != null ? calculatedTotal : BigDecimal.ZERO);
                    // 塞进新字段物理长口袋中
                    evaluation.setDynamicScoresJson(dynamicJsonResult);
                    evaluation.setAiEvaluation(dynamicJsonResult); // 备份供原有弹窗详情提取
                    
                    // 默认旧字段归零清空，防止旧数据污染
                    evaluation.setCompletenessScore(BigDecimal.ZERO);
                    evaluation.setSpecificationScore(BigDecimal.ZERO);
                    evaluation.setKnowledgeScore(BigDecimal.ZERO);
                } else {
                    // 如果老师开辟了自定义开关但没写指标，则安全滑落回传统三大指标兜底
                    AiUtils.EvaluationResult aiResult = aiUtils.evaluateReport(report.getContent());
                    fillDefaultScores(evaluation, aiResult);
                }
            } else {
                // 【传统三大指标路由】
                AiUtils.EvaluationResult aiResult = aiUtils.evaluateReport(report.getContent());
                fillDefaultScores(evaluation, aiResult);
            }
            
            // 3. 执行物理入库
            if (existing != null) {
                evaluationMapper.updateById(evaluation);
            } else {
                evaluation.setCreateTime(LocalDateTime.now());
                evaluationMapper.insert(evaluation);
            }
            
            reportService.updateStatus(reportId, Constants.REPORT_EVALUATED);
            return Result.success("AI评价完成", convertToDTO(evaluation));
            
        } catch (Exception e) {
            return Result.error("AI评价失败: " + e.getMessage());
        }
    }

    /**
     * 【全新】全格式智能评价 — 支持文档+截图+代码+ZIP混合成果
     * 输出完整7段式评价报告，存入 ai_evaluation 字段
     *
     * @param reportId  报告ID
     * @param teacherId 教师ID
     * @param rubricJson 自定义评分标准JSON（可为null，将自动生成）
     */
    @Transactional
    public Result<EvaluationDTO> aiFullEvaluate(Long reportId, Long teacherId, String rubricJson) {
        TrainingReport report = reportMapper.selectById(reportId);
        if (report == null) {
            return Result.error("报告不存在");
        }

        try {
            // 1. 收集所有关联文件的内容
            StringBuilder combinedContent = new StringBuilder();
            combinedContent.append("=== 报告标题 ===\n").append(report.getTitle()).append("\n\n");

            // 报告正文（content字段中的文本）
            if (report.getContent() != null && !report.getContent().isEmpty()) {
                combinedContent.append("=== 文档正文 ===\n").append(report.getContent()).append("\n\n");
            }

            // 附件文件（如果有）
            if (report.getFilePath() != null) {
                combinedContent.append("=== 附件文件 ===\n");
                combinedContent.append("文件名: ").append(report.getFileName()).append("\n");
                combinedContent.append("文件类型: ").append(report.getFileType()).append("\n");

                // 对于文本类文件，尝试提取内容
                if (FileUtils.isDocumentType(report.getFileType()) || FileUtils.isCodeType(report.getFileType())) {
                    try {
                        java.io.File attachedFile = new java.io.File(report.getFilePath());
                        if (attachedFile.exists()) {
                            String extractedText = FileUtils.extractTextFromFile(attachedFile);
                            if (extractedText.length() > 5000) {
                                extractedText = extractedText.substring(0, 5000) + "\n... (内容过长已截断)";
                            }
                            combinedContent.append("文件内容:\n").append(extractedText).append("\n");
                        }
                    } catch (Exception e) {
                        combinedContent.append("[文件内容提取失败: ").append(e.getMessage()).append("]\n");
                    }
                } else if (FileUtils.isImageType(report.getFileType())) {
                    combinedContent.append("[此为图片文件，AI将基于文件名和报告正文进行评价]\n");
                    combinedContent.append("图片文件名: ").append(report.getFileName()).append("\n");
                } else if (FileUtils.isArchiveType(report.getFileType())) {
                    try {
                        java.io.File zipFile = new java.io.File(report.getFilePath());
                        if (zipFile.exists()) {
                            String zipSummary = FileUtils.extractZipFileSummary(zipFile);
                            combinedContent.append(zipSummary).append("\n");
                        }
                    } catch (Exception e) {
                        combinedContent.append("[ZIP文件解析失败: ").append(e.getMessage()).append("]\n");
                    }
                }
            }

            // 2. 评分标准处理
            if (rubricJson == null || rubricJson.isEmpty()) {
                // 自动生成评分标准
                List<EvaluationCriterion> autoRubric = aiUtils.autoGenerateRubric(combinedContent.toString());
                rubricJson = JSON.toJSONString(autoRubric);
            }

            // 3. 调用AI全格式评价
            String fullReportJson = aiUtils.evaluateWithFullReport(combinedContent.toString(), rubricJson);

            // 4. 解析报告获取总分
            AiUtils.FullReportResult fullResult = aiUtils.parseFullReport(fullReportJson);
            BigDecimal totalScore = fullResult != null && fullResult.getTotalScore() != null
                    ? fullResult.getTotalScore() : BigDecimal.valueOf(60);

            // 5. 存入 evaluation_record
            LambdaQueryWrapper<EvaluationRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(EvaluationRecord::getReportId, reportId);
            EvaluationRecord existing = evaluationMapper.selectOne(wrapper);

            EvaluationRecord evaluation = (existing != null) ? existing : new EvaluationRecord();
            evaluation.setReportId(reportId);
            evaluation.setTeacherId(teacherId);
            evaluation.setIsAi(Constants.EVALUATION_AI);
            evaluation.setEvaluateTime(LocalDateTime.now());
            evaluation.setUpdateTime(LocalDateTime.now());
            evaluation.setTotalScore(totalScore);
            evaluation.setAiEvaluation(fullReportJson); // 完整7段式报告JSON
            evaluation.setDynamicScoresJson(rubricJson); // 评分标准

            // 旧字段清零（兼容）
            evaluation.setCompletenessScore(BigDecimal.ZERO);
            evaluation.setSpecificationScore(BigDecimal.ZERO);
            evaluation.setKnowledgeScore(BigDecimal.ZERO);

            if (existing != null) {
                evaluationMapper.updateById(evaluation);
            } else {
                evaluation.setCreateTime(LocalDateTime.now());
                evaluationMapper.insert(evaluation);
            }

            reportService.updateStatus(reportId, Constants.REPORT_EVALUATED);
            return Result.success("AI全格式评价完成", convertToDTO(evaluation));

        } catch (Exception e) {
            log.error("全格式AI评价失败", e);
            return Result.error("AI全格式评价失败: " + e.getMessage());
        }
    }

    /**
     * 【全新】自动生成评分标准
     * 根据报告内容自动匹配评分维度和分值
     */
    public Result<List<EvaluationCriterion>> autoGenerateRubric(Long reportId) {
        TrainingReport report = reportMapper.selectById(reportId);
        if (report == null) {
            return Result.error("报告不存在");
        }

        try {
            StringBuilder content = new StringBuilder();
            content.append("标题: ").append(report.getTitle()).append("\n");
            if (report.getContent() != null) {
                content.append(report.getContent());
            }
            if (report.getFileType() != null) {
                content.append("\n文件类型: ").append(report.getFileType());
                content.append("\n文件类别: ").append(FileUtils.getFileCategoryLabel(report.getFileType()));
            }

            List<EvaluationCriterion> rubric = aiUtils.autoGenerateRubric(content.toString());
            return Result.success("评分标准生成成功", rubric);
        } catch (Exception e) {
            log.error("自动生成评分标准失败", e);
            return Result.error("生成失败: " + e.getMessage());
        }
    }

    private void fillDefaultScores(EvaluationRecord evaluation, AiUtils.EvaluationResult aiResult) {
        evaluation.setCompletenessScore(aiResult.getCompletenessScore());
        evaluation.setSpecificationScore(aiResult.getSpecificationScore());
        evaluation.setKnowledgeScore(aiResult.getKnowledgeScore());
        evaluation.setTotalScore(aiResult.getTotalScore());
        evaluation.setAiEvaluation(aiResult.toJson());
        evaluation.setDynamicScoresJson(null); // 旧指标不启用动态JSON
    }
    
    /**
     * 原有传统手动修改评分接口（100% 保持完好，实现向下兼容）
     */
    @Transactional
    public Result<Void> manualEvaluate(Long id, BigDecimal completenessScore, BigDecimal specificationScore,
                                       BigDecimal knowledgeScore, BigDecimal totalScore, String manualEvaluation, Long teacherId) {
        EvaluationRecord evaluation = evaluationMapper.selectById(id);
        if (evaluation == null) {
            return Result.error("评价记录不存在");
        }
        
        evaluation.setCompletenessScore(completenessScore);
        evaluation.setSpecificationScore(specificationScore);
        evaluation.setKnowledgeScore(knowledgeScore);
        evaluation.setTotalScore(totalScore);
        evaluation.setManualEvaluation(manualEvaluation);
        evaluation.setTeacherId(teacherId);
        evaluation.setIsAi(Constants.EVALUATION_MANUAL);
        evaluation.setEvaluateTime(LocalDateTime.now());
        evaluation.setUpdateTime(LocalDateTime.now());
        
        evaluationMapper.updateById(evaluation);
        return Result.success();
    }

    /**
     * 【全新扩展方法】教师手动修改自定义多维度动态指标成绩接口
     * 利用开闭法则完美支撑前端变动项直接覆写 JSON 口袋
     */
    @Transactional
    public Result<Void> manualEvaluateCustom(Long id, BigDecimal totalScore, String dynamicScoresJsonStr, 
                                             String manualEvaluation, Long teacherId) {
        EvaluationRecord evaluation = evaluationMapper.selectById(id);
        if (evaluation == null) {
            return Result.error("评价记录不存在");
        }
        
        evaluation.setTotalScore(totalScore); // 更新纠偏后的综合大总分
        evaluation.setDynamicScoresJson(dynamicScoresJsonStr); // 覆写指标新数据
        evaluation.setManualEvaluation(manualEvaluation);
        evaluation.setTeacherId(teacherId);
        evaluation.setIsAi(Constants.EVALUATION_MANUAL); // 切换为人工订正标记
        evaluation.setEvaluateTime(LocalDateTime.now());
        evaluation.setUpdateTime(LocalDateTime.now());
        
        evaluationMapper.updateById(evaluation);
        return Result.success();
    }
    
    public Result<EvaluationDTO> getEvaluation(Long id) {
        EvaluationRecord evaluation = evaluationMapper.selectById(id);
        if (evaluation == null) {
            return Result.error("评价记录不存在");
        }
        
        return Result.success(convertToDTO(evaluation));
    }
    
    public Result<EvaluationDTO> getEvaluationByReport(Long reportId) {
        LambdaQueryWrapper<EvaluationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EvaluationRecord::getReportId, reportId);
        EvaluationRecord evaluation = evaluationMapper.selectOne(wrapper);
        
        if (evaluation == null) {
            return Result.error("该报告暂无评价");
        }
        
        return Result.success(convertToDTO(evaluation));
    }
    
    private EvaluationDTO convertToDTO(EvaluationRecord evaluation) {
        EvaluationDTO dto = new EvaluationDTO();
        dto.setId(evaluation.getId());
        dto.setReportId(evaluation.getReportId());
        dto.setTeacherId(evaluation.getTeacherId());
        dto.setCompletenessScore(evaluation.getCompletenessScore());
        dto.setSpecificationScore(evaluation.getSpecificationScore());
        dto.setKnowledgeScore(evaluation.getKnowledgeScore());
        dto.setTotalScore(evaluation.getTotalScore());
        dto.setAiEvaluation(evaluation.getAiEvaluation());
        dto.setManualEvaluation(evaluation.getManualEvaluation());
        dto.setIsAi(evaluation.getIsAi());
        dto.setEvaluateTime(evaluation.getEvaluateTime());
        dto.setCreateTime(evaluation.getCreateTime());
        
        // 【数据拼载安全保障】自动向前端下发动态 JSON 得分细则内容
        dto.setDynamicScoresJson(evaluation.getDynamicScoresJson());
        
        if (evaluation.getReportId() != null) {
            TrainingReport report = reportMapper.selectById(evaluation.getReportId());
            if (report != null) {
                dto.setReportTitle(report.getTitle());
                
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
            }
        }
        
        if (evaluation.getTeacherId() != null) {
            SysUser teacher = userMapper.selectById(evaluation.getTeacherId());
            if (teacher != null) {
                dto.setTeacherName(teacher.getRealName());
            }
        }
        
        return dto;
    }
}