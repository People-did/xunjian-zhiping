package com.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.dto.ReportDTO;
import com.training.entity.TrainingReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface TrainingReportMapper extends BaseMapper<TrainingReport> {
    IPage<ReportDTO> selectReportPage(Page<?> page, @Param("studentId") Long studentId, 
                                            @Param("courseId") Long courseId, @Param("classId") Long classId, @Param("status") Integer status);
    List<Map<String, Object>> selectScoreByClassId(@Param("classId") Long classId);
    List<Map<String, Object>> selectScoreByCourseId(@Param("courseId") Long courseId);
}
