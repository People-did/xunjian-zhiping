package com.training.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
public class ReportDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String className;
    private Long courseId;        // 课程ID
    private String courseName;   // 课程名称
    private String title;
    private String content;
    private String filePath;
    private String fileName;
    private String fileType;
    private Integer status;
    private String statusName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // ==================== 评价信息 ====================
    private Long evaluationId;
    private BigDecimal completenessScore;
    private BigDecimal specificationScore;
    private BigDecimal knowledgeScore;
    private BigDecimal totalScore;
    private String aiEvaluation;
    private String manualEvaluation;
    

    private String dynamicScoresJson;
    
    private LocalDateTime evaluateTime;
}