package com.training.dto;

import com.training.entity.EvaluationCriterion;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 教师级联发布作业与自定义指标的复合传输对象
 * 用于无损合拢第十五届中国软件杯 - 动态多指标数据流
 */
@Data
public class CustomRequirementDTO {
    
    // ==================== 原生老作业表字段 ====================
    private Long courseId;        // 课程ID
    private String title;         // 报告要求标题
    private String content;       // 报告要求内容
    private LocalDateTime deadline; // 截止时间
    private Integer status;       // 状态: 0-已禁用 1-进行中

    // ==================== 级联注入的自定义指标集合 ====================
    /**
     * 老师在 Vue 3 前端工作台上实时增删改、纠偏后的自定义指标列表
     */
    private List<EvaluationCriterion> criteria;
}