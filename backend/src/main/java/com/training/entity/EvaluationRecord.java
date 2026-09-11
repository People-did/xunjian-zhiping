package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("evaluation_record")
public class EvaluationRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportId;
    private Long teacherId;
    private BigDecimal completenessScore;
    private BigDecimal specificationScore;
    private BigDecimal knowledgeScore;
    private BigDecimal totalScore;
    private String aiEvaluation;
    private String manualEvaluation;
    
    /**
     * 存储自定义多维度指标打分结果的动态结构化 JSON 数据
     */
    private String dynamicScoresJson;
    
    private Integer isAi;  // 0-否 1-是
    private LocalDateTime evaluateTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}