package com.example.expensetracker.budget;

import com.example.expensetracker.budget.dto.BudgetRequest;
import com.example.expensetracker.budget.dto.BudgetResponse;
import com.example.expensetracker.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

@Tag(name = "Budgets")
@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @Operation(summary = "List budgets for a month with spent and remaining")
    @GetMapping
    public ApiResponse<List<BudgetResponse>> list(@AuthenticationPrincipal CurrentUser user,
                                     @RequestParam @Min(2000) @Max(2100) int year,
                                     @RequestParam @Min(1) @Max(12) int month) {
        return ApiResponse.ok("Budgets fetched successfully", budgetService.list(user.id(), year, month));
    }

    @Operation(summary = "Create a monthly budget (categoryId null = overall)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BudgetResponse> create(@AuthenticationPrincipal CurrentUser user,
                                 @Valid @RequestBody BudgetRequest request) {
        return ApiResponse.created("Budget created successfully", budgetService.create(user.id(), request));
    }

    @Operation(summary = "Update a budget")
    @PutMapping("/{id}")
    public ApiResponse<BudgetResponse> update(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                 @Valid @RequestBody BudgetRequest request) {
        return ApiResponse.ok("Budget updated successfully", budgetService.update(user.id(), id, request));
    }

    @Operation(summary = "Delete a budget")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        budgetService.delete(user.id(), id);
        return ApiResponse.ok("Budget deleted successfully");
    }
}
