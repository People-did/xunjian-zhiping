package com.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.dto.CourseDTO;
import com.training.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
    IPage<CourseDTO> selectCoursePage(Page<?> page, @Param("teacherId") Long teacherId, 
                                      @Param("classId") Long classId, @Param("courseName") String courseName);
    
    CourseDTO selectCourseDetail(@Param("id") Long id);
    
    List<Long> selectStudentCourseIds(@Param("studentId") Long studentId);
}
