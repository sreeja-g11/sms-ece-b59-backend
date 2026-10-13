package com.miniclub.dto;

import java.time.Instant;

public record StudentView(Long id, String name, String email, String department, Integer year, long clubCount,
                          long eventCount, Instant createdAt) {
}
