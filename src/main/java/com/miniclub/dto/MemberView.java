package com.miniclub.dto;

import java.time.Instant;

public record MemberView(Long userId, String name, String email, String department, Integer year, Instant joinedAt) {
}
