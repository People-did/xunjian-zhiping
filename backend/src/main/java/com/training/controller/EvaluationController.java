package com.training.controller;

import com.training.common.PageResult;
import com.training.common.Result;
import com.training.config.SecurityConfig;
import com.training.dto.EvaluationDTO;
import com.training.entity.EvaluationCriterion;
import com.training.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/evaluation")
@RequiredArgsConstructor
public class EvaluationController {
    
    private final EvaluationService evaluationService;
    
    @GetMapping("/list")
    public Result<PageResult<EvaluationDTO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long reportId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long courseId,
            @AuthenticationPrincipal SecurityConfig.UserPrincipal principal) {
        
        Integer currentRole = principal != null ? principal.getRole() : null;
        Long currentUserId = principal != null ? principal.getUserId() : null;
        
        return Result.success(evaluationService.pageList(pageNum, pageSize, reportId, teacherId, classId, 
                courseId, currentRole, currentUserId));
    }
    
    @GetMapping("/{id}")
    public Result<EvaluationDTO> getEvaluation(@PathVariable Long id) {
        return evaluationService.getEvaluation(id);
    }
    
    @GetMapping("/report/{reportId}")
    public Result<EvaluationDTO> getByReport(@PathVariable Long reportId) {
        return evaluationService.getEvaluationByReport(reportId);
    }
    
    /**
     * AI智能评价路由（Service层会智能判定并分流：走传统三大指标批改，还是自定义大模型指标批改）
     */
    @PostMapping("/ai/{reportId}")
    public Result<EvaluationDTO> aiEvaluate(
            @PathVariable Long reportId,
            @AuthenticationPrincipal SecurityConfig.UserPrincipal principal) {
        
        Long teacherId = principal != null ? principal.getUserId() : null;
        return evaluationService.aiEvaluate(reportId, teacherId);
    }
    
    /**
     * 【全新】全格式AI智能评价 — 支持文档+截图+代码+ZIP混合成果
     * 输出完整7段式评价报告
     */
    @PostMapping("/ai/full/{reportId}")
    public Result<EvaluationDTO> aiFullEvaluate(
            @PathVariable Long reportId,
            @RequestBody(required = false) Map<String, Object> params,
            @AuthenticationPrincipal SecurityConfig.UserPrincipal principal) {

        Long teacherId = principal != null ? principal.getUserId() : null;
        String rubricJson = null;

        if (params != null && params.containsKey("rubricJson")) {
            Object rubric = params.get("rubricJson");
            rubricJson = rubric instanceof String ? (String) rubric : com.alibaba.fastjson2.JSON.toJSONString(rubric);
        }

        return evaluationService.aiFullEvaluate(reportId, teacherId, rubricJson);
    }

    /**
     * 【全新】自动生成评分标准（根据成果内容智能推荐评分维度与分值）
     */
    @PostMapping("/rubric/auto-generate/{reportId}")
    public Result<List<EvaluationCriterion>> autoGenerateRubric(@PathVariable Long reportId) {
        return evaluationService.autoGenerateRubric(reportId);
    }

    /**
     * 【重构升级】手动修改评分路由（完美兼容：传统硬编码三大维度改分 与 前端 v-for 自由定制指标改分）
     */
    @PutMapping("/{id}")
    public Result<Void> manualEvaluate(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params,
            @AuthenticationPrincipal SecurityConfig.UserPrincipal principal) {
        
        Long teacherId = principal != null ? principal.getUserId() : null;
        String manualEvaluation = (String) params.get("manualEvaluation");
        BigDecimal totalScore = new BigDecimal(params.get("totalScore").toString());

        // 1. 安全分流：判定前端传过来的是否包含动态自定义指标包
        if (params.containsKey("dynamicScores") || params.containsKey("dynamicScoresJson")) {
            // 如果是自定义评分标准的作业，前端会直接把修改后的整个指标 map 对象或者 json 字串塞在 params 里
            Object dynamicData = params.get("dynamicScores") != null ? params.get("dynamicScores") : params.get("dynamicScoresJson");
            String dynamicScoresJsonStr = (dynamicData instanceof String) ? (String) dynamicData : com.alibaba.fastjson2.JSON.toJSONString(dynamicData);
            
            // 调用 Service 层专门针对自定义标准演进的手动改分接口
            return evaluationService.manualEvaluateCustom(id, totalScore, dynamicScoresJsonStr, manualEvaluation, teacherId);
        } else {
            // 2. 兜底兼容：如果属于未启用的传统旧作业要求，依然雷打不动地走原有的传统改分核心
            BigDecimal completenessScore = new BigDecimal(params.get("completenessScore") != null ? params.get("completenessScore").toString() : "0");
            BigDecimal specificationScore = new BigDecimal(params.get("specificationScore") != null ? params.get("specificationScore").toString() : "0");
            BigDecimal knowledgeScore = new BigDecimal(params.get("knowledgeScore") != null ? params.get("knowledgeScore").toString() : "0");
            
            return evaluationService.manualEvaluate(id, completenessScore, specificationScore, 
                    knowledgeScore, totalScore, manualEvaluation, teacherId);
        }
    }
}