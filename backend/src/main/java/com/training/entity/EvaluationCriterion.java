package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("evaluation_criterion")
public class EvaluationCriterion implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联的作业要求(report_requirement)ID
     */
    private Long requirementId;

    /**
     * 动态指标名称（如：代码质量、UI设计）
     */
    private String name;

    /**
     * 该指标的权重占比(1-100)
     */
    private Integer weight;

    /**
     * 当前单项指标的满分上限
     */
    private BigDecimal maxScore;

    /**
     * 供大模型批改和教师审阅时参考的评分细则描述词
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    // ==================== Getters and Setters ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}