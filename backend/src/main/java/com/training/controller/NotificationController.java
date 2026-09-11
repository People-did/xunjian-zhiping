package com.training.controller;

import com.training.common.PageResult;
import com.training.common.Result;
import com.training.entity.Notification;
import com.training.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取通知列表
     */
    @GetMapping("/list")
    public Result<PageResult<Notification>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        return Result.success(notificationService.getByUserId(pageNum, pageSize, userId));
    }

    /**
     * 获取未读数量
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> getUnreadCount() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.success(Map.of("count", 0));
        }
        int count = notificationService.getUnreadCount(userId);
        return Result.success(Map.of("count", count));
    }

    /**
     * 标记为已读
     */
    @PutMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.success();
    }

    /**
     * 标记所有为已读
     */
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = getCurrentUserId();
        if (userId != null) {
            notificationService.markAllAsRead(userId);
        }
        return Result.success();
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            try {
                Object principal = authentication.getPrincipal();
                // 使用反射获取userId
                var method = principal.getClass().getMethod("getUserId");
                return (Long) method.invoke(principal);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
