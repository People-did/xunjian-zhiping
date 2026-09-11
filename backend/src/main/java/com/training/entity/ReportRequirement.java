package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("report_requirement")
public class ReportRequirement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;       // 课程ID
    private String title;        // 报告要求标题
    private String content;       // 报告要求内容
    private LocalDateTime deadline;  // 截止时间
    private Integer status;      // 0-已禁用 1-进行中
    
    /**
     * 是否启用自定义评分标准：0-不启用 1-启用
     */
    private Integer hasCustomCriterion;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}