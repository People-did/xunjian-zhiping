package com.training.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.training.common.Constants;
import com.training.common.PageResult;
import com.training.common.Result;
import com.training.dto.ImportResultDTO;
import com.training.dto.UserDTO;
import com.training.dto.UserImportDTO;
import com.training.entity.SysClass;
import com.training.entity.SysUser;
import com.training.mapper.SysClassMapper;
import com.training.mapper.SysUserMapper;
import com.training.utils.PasswordEncoder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<SysUserMapper, SysUser> {
    
    private final SysUserMapper userMapper;
    private final SysClassMapper classMapper;
    private final PasswordEncoder passwordEncoder;
    
    public PageResult<UserDTO> pageList(Integer pageNum, Integer pageSize, String keyword, Integer role, Long classId) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getEmail, keyword));
        }
        if (role != null) {
            wrapper.eq(SysUser::getRole, role);
        }
        if (classId != null) {
            wrapper.eq(SysUser::getClassId, classId);
        }
        
        wrapper.orderByDesc(SysUser::getCreateTime);
        
        Page<SysUser> result = userMapper.selectPage(page, wrapper);
        List<UserDTO> dtoList = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return new PageResult<>(result.getTotal(), dtoList);
    }
    
    public List<UserDTO> getStudentsByClassId(Long classId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getRole, Constants.ROLE_STUDENT);
        if (classId != null) {
            wrapper.eq(SysUser::getClassId, classId);
        }
        return userMapper.selectList(wrapper).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<UserDTO> getTeachers() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getRole, Constants.ROLE_TEACHER);
        return userMapper.selectList(wrapper).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public Result<Void> addUser(SysUser user) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, user.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            return Result.error("用户名已存在");
        }
        
        // 默认密码
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword("123456");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        if (user.getRole() == null) {
            user.setRole(Constants.ROLE_STUDENT);
        }
        if (user.getStatus() == null) {
            user.setStatus(Constants.STATUS_ENABLED);
        }
        
        userMapper.insert(user);
        return Result.success();
    }
    
    public Result<Void> updateUser(SysUser user) {
        SysUser existing = userMapper.selectById(user.getId());
        if (existing == null) {
            return Result.error("用户不存在");
        }
        
        existing.setRealName(user.getRealName());
        existing.setRole(user.getRole());
        existing.setClassId(user.getClassId());
        existing.setEmail(user.getEmail());
        existing.setPhone(user.getPhone());
        existing.setStatus(user.getStatus());
        
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        userMapper.updateById(existing);
        return Result.success();
    }
    
    public Result<Void> deleteUser(Long id) {
        userMapper.deleteById(id);
        return Result.success();
    }
    
    public Result<Void> resetPassword(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
        return Result.success();
    }
    
    private UserDTO convertToDTO(SysUser user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setRole(user.getRole());
        dto.setRoleName(Constants.getRoleName(user.getRole()));
        dto.setClassId(user.getClassId());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setStatus(user.getStatus());
        dto.setCreateTime(user.getCreateTime());
        
        if (user.getClassId() != null) {
            SysClass sysClass = classMapper.selectById(user.getClassId());
            if (sysClass != null) {
                dto.setClassName(sysClass.getClassName());
            }
        }
        
        return dto;
    }
    
    /**
     * 批量导入用户
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<ImportResultDTO> importUsers(MultipartFile file) {
        try {
            // 获取所有班级名称->ID映射
            Map<String, Long> classNameToId = getClassNameToIdMap();
            
            // 读取Excel数据
            List<UserImportDTO> importList = EasyExcel.read(file.getInputStream())
                    .head(UserImportDTO.class)
                    .sheet()
                    .headRowNumber(1)
                    .doReadSync();
            
            if (importList == null || importList.isEmpty()) {
                return Result.error("导入文件为空");
            }
            
            int success = 0;
            int failed = 0;
            StringBuilder failReason = new StringBuilder();
            
            for (int i = 0; i < importList.size(); i++) {
                UserImportDTO item = importList.get(i);
                try {
                    // 校验必填项
                    if (item.getUsername() == null || item.getUsername().trim().isEmpty()) {
                        failed++;
                        failReason.append("第").append(i + 2).append("行: 用户名为空\n");
                        continue;
                    }
                    if (item.getRealName() == null || item.getRealName().trim().isEmpty()) {
                        failed++;
                        failReason.append("第").append(i + 2).append("行: 姓名为空\n");
                        continue;
                    }
                    
                    // 检查用户名是否已存在
                    LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(SysUser::getUsername, item.getUsername().trim());
                    if (userMapper.selectCount(wrapper) > 0) {
                        failed++;
                        failReason.append("第").append(i + 2).append("行: 用户名已存在\n");
                        continue;
                    }
                    
                    // 创建用户
                    SysUser user = new SysUser();
                    user.setUsername(item.getUsername().trim());
                    user.setRealName(item.getRealName().trim());
                    user.setPassword(passwordEncoder.encode("123456")); // 默认密码
                    user.setRole(item.getRole() != null ? item.getRole() : Constants.ROLE_STUDENT);
                    user.setEmail(item.getEmail() != null ? item.getEmail().trim() : null);
                    user.setPhone(item.getPhone() != null ? item.getPhone().trim() : null);
                    user.setStatus(Constants.STATUS_ENABLED);
                    user.setCreateTime(LocalDateTime.now());
                    
                    // 根据班级名称设置班级ID
                    if (item.getClassName() != null && !item.getClassName().trim().isEmpty()) {
                        Long classId = classNameToId.get(item.getClassName().trim());
                        if (classId != null) {
                            user.setClassId(classId);
                        } else {
                            // 班级不存在，忽略班级设置
                        }
                    }
                    
                    userMapper.insert(user);
                    success++;
                } catch (Exception e) {
                    failed++;
                    failReason.append("第").append(i + 2).append("行: ").append(e.getMessage()).append("\n");
                }
            }
            
            ImportResultDTO result = new ImportResultDTO(importList.size(), success, failed, 
                    failed > 0 ? failReason.toString() : null);
            return Result.success(result);
        } catch (IOException e) {
            return Result.error("读取文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载导入模板
     */
    public void downloadTemplate(HttpServletResponse response) {
        try {
            List<UserImportDTO> template = new ArrayList<>();
            // 添加示例数据
            UserImportDTO example1 = new UserImportDTO();
            example1.setUsername("stu001");
            example1.setRealName("张三");
            example1.setRole(3);
            example1.setClassName("软件工程2024级1班");
            example1.setEmail("stu001@example.com");
            example1.setPhone("13800138001");
            template.add(example1);
            
            UserImportDTO example2 = new UserImportDTO();
            example2.setUsername("tea001");
            example2.setRealName("李老师");
            example2.setRole(2);
            example2.setEmail("tea001@example.com");
            example2.setPhone("13800138002");
            template.add(example2);
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("用户导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
            
            EasyExcel.write(response.getOutputStream(), UserImportDTO.class)
                    .sheet("用户导入模板")
                    .doWrite(template);
        } catch (IOException e) {
            throw new RuntimeException("下载模板失败", e);
        }
    }
    
    /**
     * 获取班级名称到ID的映射
     */
    private Map<String, Long> getClassNameToIdMap() {
        List<SysClass> allClasses = classMapper.selectList(null);
        return allClasses.stream()
                .collect(Collectors.toMap(SysClass::getClassName, SysClass::getId));
    }
}
