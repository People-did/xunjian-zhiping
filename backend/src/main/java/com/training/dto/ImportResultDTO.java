package com.training.dto;

import lombok.Data;

/**
 * 批量导入结果DTO
 */
@Data
public class ImportResultDTO {
    private int total;           // 总数
    private int success;         // 成功数
    private int failed;          // 失败数
    private String failReason;   // 失败原因
    
    public ImportResultDTO() {}
    
    public ImportResultDTO(int total, int success, int failed, String failReason) {
        this.total = total;
        this.success = success;
        this.failed = failed;
        this.failReason = failReason;
    }
}
