package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String courseName;
    private String description;
    private Long teacherId;
    private Long classId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 扩展字段（用于显示，非数据库字段）
    @TableField(exist = false)
    private String teacherName;
    
    @TableField(exist = false)
    private String className;
}
