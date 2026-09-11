package com.training.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.common.Constants;
import com.training.common.PageResult;
import com.training.common.Result;
import com.training.dto.ImportResultDTO;
import com.training.dto.UserDTO;
import com.training.entity.SysUser;
import com.training.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/list")
    public Result<PageResult<UserDTO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) Long classId) {
        
        return Result.success(userService.pageList(pageNum, pageSize, keyword, role, classId));
    }
    
    @GetMapping("/students")
    public Result<List<UserDTO>> getStudents(@RequestParam(required = false) Long classId) {
        return Result.success(userService.getStudentsByClassId(classId));
    }
    
    @GetMapping("/teachers")
    public Result<List<UserDTO>> getTeachers() {
        return Result.success(userService.getTeachers());
    }
    
    @PostMapping
    public Result<Void> add(@RequestBody SysUser user) {
        return userService.addUser(user);
    }
    
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        return userService.updateUser(user);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return userService.deleteUser(id);
    }
    
    @PostMapping("/reset-pwd/{id}")
    public Result<Void> resetPassword(@PathVariable Long id) {
        return userService.resetPassword(id);
    }
    
    /**
     * 批量导入用户
     */
    @PostMapping("/import")
    public Result<ImportResultDTO> importUsers(@RequestParam("file") MultipartFile file) {
        return userService.importUsers(file);
    }
    
    /**
     * 下载导入模板
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        userService.downloadTemplate(response);
    }
}
