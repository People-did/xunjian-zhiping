package com.training.dto;

import lombok.Data;

@Data
public class ScoreDistribution {
    private String range;
    private Integer count;
    private Double percentage;
}
