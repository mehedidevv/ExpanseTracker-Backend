package com.example.expensetracker.transaction;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.expensetracker.category.Category;
import com.example.expensetracker.category.CategoryService;
import com.example.expensetracker.category.CategoryType;
import com.example.expensetracker.common.exception.BadRequestException;
import com.example.expensetracker.transaction.dto.CreateTransactionRequest;
import com.example.expensetracker.transaction.dto.TransactionFilter;
import com.example.expensetracker.user.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class TransactionServiceTest {

    private final TransactionRepository repository = mock(TransactionRepository.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final UserService userService = mock(UserService.class);
    private TransactionService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TransactionService(repository, categoryService, userService, new TransactionMapper());
    }

    @Test
    void rejectsTypeThatDoesNotMatchCategory() {
        UUID categoryId = UUID.randomUUID();
        Category income = mock(Category.class);
        when(income.getType()).thenReturn(CategoryType.INCOME);
        when(categoryService.getUsable(userId, categoryId)).thenReturn(income);

        var req = new CreateTransactionRequest(CategoryType.EXPENSE, new BigDecimal("5.00"), categoryId,
                LocalDate.now(), null);

        assertThatThrownBy(() -> service.create(userId, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match");
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsUnknownSortProperty() {
        var page = PageRequest.of(0, 20, Sort.by("user.password"));
        assertThatThrownBy(() -> service.list(userId, null, page))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot sort by");
    }

    @Test
    void rejectsInvertedDateRange() {
        var filter = new TransactionFilter(null, null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1),
                null, null, null);
        assertThatThrownBy(() -> service.list(userId, filter, PageRequest.of(0, 20)))
                .isInstanceOf(BadRequestException.class);
    }
}
