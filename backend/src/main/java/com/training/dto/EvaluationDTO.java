package com.training.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EvaluationDTO {
    private Long id;
    private Long reportId;
    private String reportTitle;
    private String studentName;
    private String className;
    private Long teacherId;
    private String teacherName;
    private BigDecimal completenessScore;
    private BigDecimal specificationScore;
    private BigDecimal knowledgeScore;
    private BigDecimal totalScore;
    private String aiEvaluation;
    private String manualEvaluation;
    
    /**
     * 存储自定义多维度指标打分结果的动态结构化 JSON 数据
     * 作用：用于承载大模型打分或教师手动改分后的新多维指标包数据传输
     */
    private String dynamicScoresJson;
    
    private Integer isAi;
    private LocalDateTime evaluateTime;
    private LocalDateTime createTime;
}