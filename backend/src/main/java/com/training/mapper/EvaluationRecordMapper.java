package com.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.training.entity.EvaluationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface EvaluationRecordMapper extends BaseMapper<EvaluationRecord> {
    Map<String, Object> selectClassStatistics(@Param("classId") Long classId);
    List<Map<String, Object>> selectScoreDistribution(@Param("classId") Long classId);
    List<Map<String, Object>> selectAllScores(@Param("classId") Long classId);
}
