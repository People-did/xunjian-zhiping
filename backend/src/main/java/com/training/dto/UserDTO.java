package com.training.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String realName;
    private Integer role;
    private String roleName;
    private Long classId;
    private String className;
    private String email;
    private String phone;
    private Integer status;
    private LocalDateTime createTime;
}
