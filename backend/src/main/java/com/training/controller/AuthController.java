package com.training.controller;

import com.training.common.Result;
import com.training.config.SecurityConfig;
import com.training.dto.LoginRequest;
import com.training.dto.LoginResponse;
import com.training.dto.UserDTO;
import com.training.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
    
    @GetMapping("/userinfo")
    public Result<UserDTO> getUserInfo(@AuthenticationPrincipal SecurityConfig.UserPrincipal principal) {
        return authService.getUserInfo(principal.getUserId());
    }
    
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
