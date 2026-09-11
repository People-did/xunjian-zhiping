package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private Integer role;  // 1-管理员 2-教师 3-学生
    private Long classId;
    private String email;
    private String phone;
    private Integer status;  // 0-禁用 1-启用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
