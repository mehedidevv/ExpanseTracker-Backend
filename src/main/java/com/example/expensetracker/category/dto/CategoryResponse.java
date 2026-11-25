package com.example.expensetracker.category.dto;

import com.example.expensetracker.category.CategoryType;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, CategoryType type, String icon, String color,
                               boolean systemDefault) {
}
