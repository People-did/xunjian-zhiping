package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;         // 通知用户ID
    private Integer type;        // 1-报告通知 2-系统通知
    private String title;        // 通知标题
    private String content;      // 通知内容
    private Long relatedId;      // 关联ID
    private Integer isRead;      // 0-未读 1-已读
    private LocalDateTime createTime;
}
