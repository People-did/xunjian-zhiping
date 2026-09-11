package com.training.dto;

import lombok.Data;
import java.util.Map;

@Data
public class AiEvaluationRequest {
    private String model;
    private String prompt;
    private Map<String, Object> evaluationResult;
}
