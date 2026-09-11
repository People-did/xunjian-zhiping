package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_class")
public class SysClass {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String className;
    private Long teacherId;
    private String description;
    private LocalDateTime createTime;
}
