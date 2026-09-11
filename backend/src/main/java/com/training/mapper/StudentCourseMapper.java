package com.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.training.entity.StudentCourse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentCourseMapper extends BaseMapper<StudentCourse> {
    List<Long> selectCourseIdsByStudentId(@Param("studentId") Long studentId);
    
    List<Long> selectStudentIdsByCourseId(@Param("courseId") Long courseId);
    
    int countByCourseId(@Param("courseId") Long courseId);
}
