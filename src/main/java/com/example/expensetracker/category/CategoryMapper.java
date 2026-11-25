package com.example.expensetracker.category;

import com.example.expensetracker.category.dto.CategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getType(), c.getIcon(), c.getColor(),
                c.isSystemDefault());
    }
}
