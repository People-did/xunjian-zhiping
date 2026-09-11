package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("training_report")
public class TrainingReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long courseId;    // 课程ID
    private String title;
    private String content;
    private String filePath;
    private String fileName;
    private String fileType;
    private Integer status;  // 0-待评价 1-已评价
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
