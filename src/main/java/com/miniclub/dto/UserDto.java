package com.miniclub.dto;

import com.miniclub.model.Role;
import com.miniclub.model.User;
import java.time.Instant;

public record UserDto(Long id, String name, String email, Role role, String department, Integer year,
                      Instant createdAt) {

    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getDepartment(), u.getYear(),
                u.getCreatedAt());
    }
}
