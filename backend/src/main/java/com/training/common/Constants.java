package com.training.common;

public class Constants {
    // 角色
    public static final int ROLE_ADMIN = 1;
    public static final int ROLE_TEACHER = 2;
    public static final int ROLE_STUDENT = 3;
    
    // 状态
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;
    
    // 报告状态
    public static final int REPORT_PENDING = 0;
    public static final int REPORT_EVALUATED = 1;
    
    // 评价类型
    public static final int EVALUATION_MANUAL = 0;
    public static final int EVALUATION_AI = 1;
    
    // 角色名称
    public static String getRoleName(Integer role) {
        if (role == null) return "未知";
        switch (role) {
            case ROLE_ADMIN: return "管理员";
            case ROLE_TEACHER: return "教师";
            case ROLE_STUDENT: return "学生";
            default: return "未知";
        }
    }
    
    // 报告状态名称
    public static String getReportStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case REPORT_PENDING: return "待评价";
            case REPORT_EVALUATED: return "已评价";
            default: return "未知";
        }
    }
}
