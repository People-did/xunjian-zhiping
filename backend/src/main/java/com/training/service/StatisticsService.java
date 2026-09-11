package com.training.service;

import com.alibaba.excel.EasyExcel;
import com.training.common.Result;
import com.training.dto.ScoreDistribution;
import com.training.dto.StatisticsDTO;
import com.training.entity.SysClass;
import com.training.entity.SysUser;
import com.training.entity.TrainingReport;
import com.training.mapper.EvaluationRecordMapper;
import com.training.mapper.SysClassMapper;
import com.training.mapper.SysUserMapper;
import com.training.mapper.TrainingReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    
    private final SysClassMapper classMapper;
    private final SysUserMapper userMapper;
    private final TrainingReportMapper reportMapper;
    private final EvaluationRecordMapper evaluationMapper;
    
    public Result<StatisticsDTO> getClassStatistics(Long classId) {
        SysClass sysClass = classMapper.selectById(classId);
        if (sysClass == null) {
            return Result.error("班级不存在");
        }
        
        StatisticsDTO dto = new StatisticsDTO();
        dto.setClassId(classId);
        dto.setClassName(sysClass.getClassName());
        
        // 统计学生人数
        List<SysUser> students = userMapper.selectByClassId(classId);
        dto.setTotalStudents(students.size());
        
        // 统计已评价的报告
        List<Map<String, Object>> scores = evaluationMapper.selectAllScores(classId);
        
        dto.setTotalReports(scores.size());
        
        int evaluatedCount = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;
        BigDecimal minScore = BigDecimal.valueOf(100);
        
        for (Map<String, Object> score : scores) {
            Object totalObj = score.get("total_score");
            if (totalObj != null) {
                BigDecimal scoreValue = new BigDecimal(totalObj.toString());
                evaluatedCount++;
                totalScore = totalScore.add(scoreValue);
                
                if (scoreValue.compareTo(maxScore) > 0) {
                    maxScore = scoreValue;
                }
                if (scoreValue.compareTo(minScore) < 0) {
                    minScore = scoreValue;
                }
            }
        }
        
        dto.setEvaluatedReports(evaluatedCount);
        
        if (evaluatedCount > 0) {
            dto.setAvgScore(totalScore.divide(BigDecimal.valueOf(evaluatedCount), 2, RoundingMode.HALF_UP));
        } else {
            dto.setAvgScore(BigDecimal.ZERO);
        }
        
        dto.setMaxScore(evaluatedCount > 0 ? maxScore : BigDecimal.ZERO);
        dto.setMinScore(evaluatedCount > 0 ? minScore : BigDecimal.ZERO);
        
        // 计算分数段分布
        dto.setScoreDistribution(calculateScoreDistribution(scores));
        
        // 成绩列表
        dto.setScoreList(scores);
        
        return Result.success(dto);
    }
    
    public Result<List<Map<String, Object>>> getAllClassesStatistics() {
        List<SysClass> classes = classMapper.selectList(null);
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (SysClass sysClass : classes) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("classId", sysClass.getId());
            stat.put("className", sysClass.getClassName());
            
            List<Map<String, Object>> scores = evaluationMapper.selectAllScores(sysClass.getId());
            
            int evaluatedCount = 0;
            BigDecimal totalScore = BigDecimal.ZERO;
            BigDecimal maxScore = BigDecimal.ZERO;
            BigDecimal minScore = BigDecimal.valueOf(100);
            
            for (Map<String, Object> score : scores) {
                Object totalObj = score.get("total_score");
                if (totalObj != null) {
                    BigDecimal scoreValue = new BigDecimal(totalObj.toString());
                    evaluatedCount++;
                    totalScore = totalScore.add(scoreValue);
                    
                    if (scoreValue.compareTo(maxScore) > 0) {
                        maxScore = scoreValue;
                    }
                    if (scoreValue.compareTo(minScore) < 0) {
                        minScore = scoreValue;
                    }
                }
            }
            
            stat.put("totalReports", scores.size());
            stat.put("evaluatedReports", evaluatedCount);
            stat.put("avgScore", evaluatedCount > 0 
                    ? totalScore.divide(BigDecimal.valueOf(evaluatedCount), 2, RoundingMode.HALF_UP) 
                    : BigDecimal.ZERO);
            stat.put("maxScore", evaluatedCount > 0 ? maxScore : BigDecimal.ZERO);
            stat.put("minScore", evaluatedCount > 0 ? minScore : BigDecimal.ZERO);
            
            result.add(stat);
        }
        
        return Result.success(result);
    }
    
    private List<ScoreDistribution> calculateScoreDistribution(List<Map<String, Object>> scores) {
        int[] ranges = {0, 0, 0, 0, 0}; // 0-60, 60-70, 70-80, 80-90, 90-100
        
        for (Map<String, Object> score : scores) {
            Object totalObj = score.get("total_score");
            if (totalObj != null) {
                BigDecimal scoreValue = new BigDecimal(totalObj.toString());
                double s = scoreValue.doubleValue();
                
                if (s < 60) {
                    ranges[0]++;
                } else if (s < 70) {
                    ranges[1]++;
                } else if (s < 80) {
                    ranges[2]++;
                } else if (s < 90) {
                    ranges[3]++;
                } else {
                    ranges[4]++;
                }
            }
        }
        
        int total = scores.size();
        List<ScoreDistribution> distributions = new ArrayList<>();
        String[] labels = {"0-60分", "60-70分", "70-80分", "80-90分", "90-100分"};
        
        for (int i = 0; i < 5; i++) {
            ScoreDistribution dist = new ScoreDistribution();
            dist.setRange(labels[i]);
            dist.setCount(ranges[i]);
            dist.setPercentage(total > 0 ? (double) Math.round(ranges[i] * 100.0 / total) : 0.0);
            distributions.add(dist);
        }
        
        return distributions;
    }
    
    public Result<Map<String, Object>> exportScores(Long classId, String exportPath) {
        Result<StatisticsDTO> statResult = getClassStatistics(classId);
        if (statResult.getCode() != 200) {
            return Result.error("导出失败");
        }
        
        StatisticsDTO dto = statResult.getData();
        
        List<Map<String, Object>> data = dto.getScoreList();
        
        // 写入Excel
        String filename = dto.getClassName() + "_成绩单_" + System.currentTimeMillis() + ".xlsx";
        String fullPath = exportPath + "/" + filename;
        
        try {
            // 创建目录
            new java.io.File(exportPath).mkdirs();
            
            EasyExcel.write(fullPath)
                    .head(createHeaders())
                    .sheet("成绩单")
                    .doWrite(data);
            
            Map<String, Object> result = new HashMap<>();
            result.put("filename", filename);
            result.put("path", fullPath);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("导出失败: " + e.getMessage());
        }
    }
    
    private List<List<String>> createHeaders() {
        List<List<String>> headers = new ArrayList<>();
        headers.add(Arrays.asList("学号", "姓名", "班级", "报告标题", "完整性得分", "规范性得分", "知识点得分", "总分", "评价时间"));
        return headers;
    }
}
