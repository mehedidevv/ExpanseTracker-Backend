package com.example.expensetracker.category.dto;

import com.example.expensetracker.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull CategoryType type,
        @Size(max = 30) String icon,
        @Size(max = 30) String color) {
}
