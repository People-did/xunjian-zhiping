package com.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.training.common.Constants;
import com.training.common.Result;
import com.training.dto.LoginRequest;
import com.training.dto.LoginResponse;
import com.training.dto.UserDTO;
import com.training.entity.SysClass;
import com.training.entity.SysUser;
import com.training.mapper.SysClassMapper;
import com.training.mapper.SysUserMapper;
import com.training.utils.JwtUtils;
import com.training.utils.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final SysUserMapper userMapper;
    private final SysClassMapper classMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    
    public Result<LoginResponse> login(LoginRequest request) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        if (user.getStatus() == Constants.STATUS_DISABLED) {
            return Result.error("用户已被禁用");
        }
        
        // 临时：支持明文密码匹配，方便调试
        String inputPassword = request.getPassword();
        String storedPassword = user.getPassword();
        boolean passwordMatch = inputPassword.equals(storedPassword) || 
                                passwordEncoder.matches(inputPassword, storedPassword);
        
        if (!passwordMatch) {
            return Result.error("密码错误");
        }
        
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole());
        response.setRoleName(Constants.getRoleName(user.getRole()));
        
        return Result.success("登录成功", response);
    }
    
    public Result<UserDTO> getUserInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
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
        
        return Result.success(dto);
    }
}
