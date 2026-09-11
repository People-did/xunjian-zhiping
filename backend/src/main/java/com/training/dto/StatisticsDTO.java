package com.training.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class StatisticsDTO {
    private Long classId;
    private String className;
    private Integer totalStudents;
    private Integer totalReports;
    private Integer evaluatedReports;
    private BigDecimal avgScore;
    private BigDecimal maxScore;
    private BigDecimal minScore;
    private List<ScoreDistribution> scoreDistribution;
    private List<Map<String, Object>> scoreList;
}
