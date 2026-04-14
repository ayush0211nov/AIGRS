package com.aigrs.backend.dto.response;

import com.aigrs.backend.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private UUID orgId;
    private String avatarUrl;
    private Boolean isActive;
    private String departmentName;
    private UUID departmentId;
    private Boolean emailNotificationsEnabled;
    private Boolean smsNotificationsEnabled;
    private Boolean pushNotificationsEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
