package com.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.training.common.Constants;
import com.training.common.PageResult;
import com.training.common.Result;
import com.training.dto.UserDTO;
import com.training.entity.SysClass;
import com.training.entity.SysUser;
import com.training.mapper.SysClassMapper;
import com.training.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassService extends ServiceImpl<SysClassMapper, SysClass> {
    
    private final SysClassMapper classMapper;
    private final SysUserMapper userMapper;
    
    public PageResult<SysClass> pageList(Integer pageNum, Integer pageSize, String keyword) {
        Page<SysClass> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysClass> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysClass::getClassName, keyword);
        }
        
        wrapper.orderByDesc(SysClass::getCreateTime);
        
        Page<SysClass> result = classMapper.selectPage(page, wrapper);
        
        // 填充班主任名称
        List<SysClass> records = result.getRecords();
        for (SysClass record : records) {
            if (record.getTeacherId() != null) {
                SysUser teacher = userMapper.selectById(record.getTeacherId());
                if (teacher != null) {
                    record.setDescription(teacher.getRealName()); // 临时存储教师姓名
                }
            }
        }
        
        return new PageResult<>(result.getTotal(), records);
    }
    
    public List<SysClass> listAll() {
        return classMapper.selectList(null);
    }
    
    /**
     * 获取教师所教的班级列表（含统计信息）
     */
    public List<Map<String, Object>> getTeacherClasses(Long teacherId) {
        // 查找该教师负责的班级
        LambdaQueryWrapper<SysClass> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysClass::getTeacherId, teacherId);
        List<SysClass> classes = classMapper.selectList(wrapper);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysClass cls : classes) {
            Map<String, Object> classInfo = new HashMap<>();
            classInfo.put("id", cls.getId());
            classInfo.put("className", cls.getClassName());
            classInfo.put("description", cls.getDescription());
            classInfo.put("teacherId", cls.getTeacherId());
            
            // 统计学生人数
            LambdaQueryWrapper<SysUser> studentWrapper = new LambdaQueryWrapper<>();
            studentWrapper.eq(SysUser::getClassId, cls.getId()).eq(SysUser::getRole, Constants.ROLE_STUDENT);
            classInfo.put("studentCount", userMapper.selectCount(studentWrapper));
            
            result.add(classInfo);
        }
        
        return result;
    }
    
    /**
     * 获取班级详细信息（含统计）
     */
    public Map<String, Object> getClassDetail(Long classId) {
        Map<String, Object> result = new HashMap<>();
        
        SysClass cls = classMapper.selectById(classId);
        if (cls == null) {
            return result;
        }
        
        result.put("id", cls.getId());
        result.put("className", cls.getClassName());
        result.put("description", cls.getDescription());
        
        // 获取班主任信息
        if (cls.getTeacherId() != null) {
            SysUser teacher = userMapper.selectById(cls.getTeacherId());
            if (teacher != null) {
                result.put("teacherName", teacher.getRealName());
            }
        }
        
        // 统计学生人数
        LambdaQueryWrapper<SysUser> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(SysUser::getClassId, classId).eq(SysUser::getRole, Constants.ROLE_STUDENT);
        List<SysUser> students = userMapper.selectList(studentWrapper);
        result.put("studentCount", students.size());
        result.put("students", students);
        
        return result;
    }
    
    public Result<Void> addClass(SysClass sysClass) {
        classMapper.insert(sysClass);
        return Result.success();
    }
    
    public Result<Void> updateClass(SysClass sysClass) {
        classMapper.updateById(sysClass);
        return Result.success();
    }
    
    public Result<Void> deleteClass(Long id) {
        // 检查是否有学生
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getClassId, id);
        if (userMapper.selectCount(wrapper) > 0) {
            return Result.error("该班级下存在学生，无法删除");
        }
        
        classMapper.deleteById(id);
        return Result.success();
    }
    
    public List<UserDTO> getClassStudents(Long classId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getClassId, classId)
               .eq(SysUser::getRole, 3); // 学生角色
        
        return userMapper.selectList(wrapper).stream()
                .map(user -> {
                    UserDTO dto = new UserDTO();
                    dto.setId(user.getId());
                    dto.setUsername(user.getUsername());
                    dto.setRealName(user.getRealName());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
