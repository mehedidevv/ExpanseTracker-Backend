package com.example.expensetracker.transaction;

import com.example.expensetracker.common.response.PageResponse;
import com.example.expensetracker.common.security.CurrentUser;
import com.example.expensetracker.transaction.dto.CreateTransactionRequest;
import com.example.expensetracker.transaction.dto.TransactionFilter;
import com.example.expensetracker.transaction.dto.TransactionResponse;
import com.example.expensetracker.transaction.dto.UpdateTransactionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Transactions")
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(summary = "List transactions (paged, filtered). Default sort: transactionDate,desc")
    @GetMapping
    public ApiResponse<PageResponse<TransactionResponse>> list(@AuthenticationPrincipal CurrentUser user,
                                                  @ParameterObject TransactionFilter filter,
                                                  @ParameterObject Pageable pageable) {
        return ApiResponse.ok("Transactions fetched successfully", transactionService.list(user.id(), filter, pageable));
    }

    @Operation(summary = "Create a transaction")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResponse> create(@AuthenticationPrincipal CurrentUser user,
                                      @Valid @RequestBody CreateTransactionRequest request) {
        return ApiResponse.created("Transaction created successfully", transactionService.create(user.id(), request));
    }

    @Operation(summary = "Get one transaction")
    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> get(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return ApiResponse.ok("Transaction fetched successfully", transactionService.get(user.id(), id));
    }

    @Operation(summary = "Update a transaction")
    @PutMapping("/{id}")
    public ApiResponse<TransactionResponse> update(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                      @Valid @RequestBody UpdateTransactionRequest request) {
        return ApiResponse.ok("Transaction updated successfully", transactionService.update(user.id(), id, request));
    }

    @Operation(summary = "Delete a transaction")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        transactionService.delete(user.id(), id);
        return ApiResponse.ok("Transaction deleted successfully");
    }
}
