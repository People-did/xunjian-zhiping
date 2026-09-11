package com.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.training.entity.EvaluationCriterion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 动态评分标准指标明细 Mapper 接口
 */
@Mapper
public interface EvaluationCriterionMapper extends BaseMapper<EvaluationCriterion> {
}