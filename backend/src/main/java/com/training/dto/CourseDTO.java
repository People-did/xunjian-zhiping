package com.training.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CourseDTO {
    private Long id;
    private String courseName;
    private String description;
    private Long teacherId;
    private String teacherName;
    private Long classId;
    private String className;
    private LocalDateTime createTime;
    
    // 统计信息
    private Integer totalStudents;    // 选修人数
    private Integer totalReports;    // 提交报告数
    private Integer evaluatedReports; // 已评价数
    private Double avgScore;         // 平均分
}
