package com.example.expensetracker.category;

import com.example.expensetracker.category.dto.CategoryRequest;
import com.example.expensetracker.category.dto.CategoryResponse;
import com.example.expensetracker.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import com.example.expensetracker.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Categories")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "List system defaults + own categories")
    @GetMapping
    public ApiResponse<List<CategoryResponse>> list(@AuthenticationPrincipal CurrentUser user,
                                       @RequestParam(required = false) CategoryType type) {
        return ApiResponse.ok("Categories fetched successfully", categoryService.list(user.id(), type));
    }

    @Operation(summary = "Create a custom category")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> create(@AuthenticationPrincipal CurrentUser user,
                                   @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.created("Category created successfully", categoryService.create(user.id(), request));
    }

    @Operation(summary = "Update a custom category")
    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                   @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok("Category updated successfully", categoryService.update(user.id(), id, request));
    }

    @Operation(summary = "Delete a custom category (409 if in use)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        categoryService.delete(user.id(), id);
        return ApiResponse.ok("Category deleted successfully");
    }
}
