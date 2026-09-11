package com.training.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 用户导入模板DTO
 */
@Data
public class UserImportDTO {
    @ExcelProperty("用户名")
    private String username;
    
    @ExcelProperty("姓名")
    private String realName;
    
    @ExcelProperty("角色（1-管理员 2-教师 3-学生）")
    private Integer role;
    
    @ExcelProperty("班级名称")
    private String className;
    
    @ExcelProperty("邮箱")
    private String email;
    
    @ExcelProperty("电话")
    private String phone;
}
