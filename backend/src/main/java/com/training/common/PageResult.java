package com.training.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private Long total;
    private List<T> records;
    
    public PageResult(Long total, List<T> records) {
        this.total = total;
        this.records = records;
    }
}
