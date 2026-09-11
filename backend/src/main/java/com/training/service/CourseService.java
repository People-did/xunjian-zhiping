package com.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.training.common.Constants;
import com.training.common.PageResult;
import com.training.common.Result;
import com.training.dto.CourseDTO;
import com.training.entity.Course;
import com.training.entity.StudentCourse;
import com.training.entity.SysUser;
import com.training.mapper.CourseMapper;
import com.training.mapper.StudentCourseMapper;
import com.training.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService extends ServiceImpl<CourseMapper, Course> {
    
    private final CourseMapper courseMapper;
    private final StudentCourseMapper studentCourseMapper;
    private final SysUserMapper userMapper;
    
    public PageResult<CourseDTO> pageList(Integer pageNum, Integer pageSize, Long teacherId, 
                                          Long classId, String courseName, Long currentUserId, Integer currentRole) {
        Page<Course> page = new Page<>(pageNum, pageSize);
        
        // 教师只能查看自己的课程
        if (currentRole != null && currentRole == Constants.ROLE_TEACHER) {
            teacherId = currentUserId;
        }
        
        IPage<CourseDTO> result = courseMapper.selectCoursePage(page, teacherId, classId, courseName);
        
        return new PageResult<>(result.getTotal(), result.getRecords());
    }
    
    public Result<CourseDTO> getCourse(Long id) {
        CourseDTO dto = courseMapper.selectCourseDetail(id);
        if (dto == null) {
            return Result.error("课程不存在");
        }
        return Result.success(dto);
    }
    
    public Result<Void> addCourse(Course course) {
        // 验证教师是否存在
        if (course.getTeacherId() != null) {
            SysUser teacher = userMapper.selectById(course.getTeacherId());
            if (teacher == null || teacher.getRole() != Constants.ROLE_TEACHER) {
                return Result.error("教师不存在");
            }
        }
        
        course.setCreateTime(LocalDateTime.now());
        courseMapper.insert(course);
        
        return Result.success();
    }
    
    public Result<Void> updateCourse(Course course) {
        if (course.getId() == null) {
            return Result.error("课程ID不能为空");
        }
        
        Course existing = courseMapper.selectById(course.getId());
        if (existing == null) {
            return Result.error("课程不存在");
        }
        
        course.setUpdateTime(LocalDateTime.now());
        courseMapper.updateById(course);
        
        return Result.success();
    }
    
    public Result<Void> deleteCourse(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            return Result.error("课程不存在");
        }
        
        // 删除选课记录
        LambdaQueryWrapper<StudentCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentCourse::getCourseId, id);
        studentCourseMapper.delete(wrapper);
        
        // 删除课程
        courseMapper.deleteById(id);
        
        return Result.success();
    }
    
    /**
     * 获取学生可选的课程列表
     */
    public Result<List<CourseDTO>> getAvailableCourses(Long studentId) {
        // 获取该学生已选修的课程ID
        List<Long> selectedCourseIds = courseMapper.selectStudentCourseIds(studentId);
        
        // 获取该学生班级对应的所有课程
        SysUser student = userMapper.selectById(studentId);
        if (student == null || student.getClassId() == null) {
            return Result.success(List.of());
        }
        
        Page<Course> page = new Page<>(1, 100);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getClassId, student.getClassId());
        IPage<Course> coursePage = courseMapper.selectPage(page, wrapper);
        
        // 转换为DTO并标记已选课程
        List<CourseDTO> result = coursePage.getRecords().stream().map(course -> {
            CourseDTO dto = new CourseDTO();
            dto.setId(course.getId());
            dto.setCourseName(course.getCourseName());
            dto.setDescription(course.getDescription());
            dto.setTeacherId(course.getTeacherId());
            dto.setClassId(course.getClassId());
            dto.setCreateTime(course.getCreateTime());
            
            // 获取教师名称
            if (course.getTeacherId() != null) {
                SysUser teacher = userMapper.selectById(course.getTeacherId());
                if (teacher != null) {
                    dto.setTeacherName(teacher.getRealName());
                }
            }
            
            // 标记是否已选
            dto.setTotalStudents(studentCourseMapper.countByCourseId(course.getId()));
            
            return dto;
        }).toList();
        
        return Result.success(result);
    }
    
    /**
     * 学生选课
     */
    @Transactional
    public Result<Void> selectCourse(Long studentId, Long courseId) {
        // 验证课程是否存在
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            return Result.error("课程不存在");
        }
        
        // 验证学生是否已在该班级
        SysUser student = userMapper.selectById(studentId);
        if (student == null || !course.getClassId().equals(student.getClassId())) {
            return Result.error("您不在该课程所属班级，无法选课");
        }
        
        // 检查是否已选
        LambdaQueryWrapper<StudentCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentCourse::getStudentId, studentId)
               .eq(StudentCourse::getCourseId, courseId);
        long count = studentCourseMapper.selectCount(wrapper);
        if (count > 0) {
            return Result.error("您已选修该课程");
        }
        
        // 添加选课记录
        StudentCourse sc = new StudentCourse();
        sc.setStudentId(studentId);
        sc.setCourseId(courseId);
        sc.setCreateTime(LocalDateTime.now());
        studentCourseMapper.insert(sc);
        
        return Result.success();
    }
    
    /**
     * 学生取消选课
     */
    @Transactional
    public Result<Void> cancelCourse(Long studentId, Long courseId) {
        LambdaQueryWrapper<StudentCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentCourse::getStudentId, studentId)
               .eq(StudentCourse::getCourseId, courseId);
        studentCourseMapper.delete(wrapper);
        
        return Result.success();
    }
    
    /**
     * 获取学生已选的课程列表
     */
    public Result<List<Course>> getStudentCourses(Long studentId) {
        List<Long> courseIds = courseMapper.selectStudentCourseIds(studentId);
        if (courseIds.isEmpty()) {
            return Result.success(List.of());
        }
        
        List<Course> courses = courseMapper.selectList(
            new LambdaQueryWrapper<Course>().in(Course::getId, courseIds)
        );
        
        // 填充教师名称
        for (Course course : courses) {
            if (course.getTeacherId() != null) {
                SysUser teacher = userMapper.selectById(course.getTeacherId());
                if (teacher != null) {
                    course.setTeacherName(teacher.getRealName());
                }
            }
        }
        
        return Result.success(courses);
    }
    
    /**
     * 获取教师被分配的课程列表
     */
    public Result<List<Course>> getTeacherCourses(Long teacherId) {
        List<Course> courses = courseMapper.selectList(
            new LambdaQueryWrapper<Course>().eq(Course::getTeacherId, teacherId)
        );
        
        // 填充班级名称
        for (Course course : courses) {
            if (course.getClassId() != null) {
                SysUser student = userMapper.selectById(course.getClassId());
                // 班级名称需要从SysClass获取，这里暂时省略
            }
        }
        
        return Result.success(courses);
    }
    
    /**
     * 获取课程下的学生列表
     */
    public Result<List<SysUser>> getCourseStudents(Long courseId) {
        List<Long> studentIds = studentCourseMapper.selectStudentIdsByCourseId(courseId);
        if (studentIds.isEmpty()) {
            return Result.success(List.of());
        }
        
        List<SysUser> students = userMapper.selectList(
            new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, studentIds)
                .eq(SysUser::getRole, Constants.ROLE_STUDENT)
        );
        
        return Result.success(students);
    }
}
