package com.example.expensetracker.category;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.expensetracker.budget.BudgetRepository;
import com.example.expensetracker.category.dto.CategoryRequest;
import com.example.expensetracker.common.exception.ConflictException;
import com.example.expensetracker.common.exception.ForbiddenException;
import com.example.expensetracker.common.exception.ResourceNotFoundException;
import com.example.expensetracker.transaction.TransactionRepository;
import com.example.expensetracker.user.User;
import com.example.expensetracker.user.UserService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryServiceTest {

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final BudgetRepository budgetRepository = mock(BudgetRepository.class);
    private CategoryService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository, transactionRepository, budgetRepository,
                mock(UserService.class), new CategoryMapper());
    }

    @Test
    void systemDefaultCannotBeDeleted() {
        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(new Category(null, "Food", CategoryType.EXPENSE, null, null)));
        assertThatThrownBy(() -> service.delete(userId, categoryId)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void otherUsersCategoryLooksMissing() {
        User other = mock(User.class);
        when(other.getId()).thenReturn(UUID.randomUUID());
        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(new Category(other, "Pets", CategoryType.EXPENSE, null, null)));
        assertThatThrownBy(() -> service.delete(userId, categoryId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void categoryWithTransactionsCannotBeDeleted() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(userId);
        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(new Category(owner, "Pets", CategoryType.EXPENSE, null, null)));
        when(transactionRepository.existsByCategoryId(categoryId)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(userId, categoryId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void duplicateNameIsConflict() {
        when(categoryRepository.existsVisibleByName(userId, CategoryType.EXPENSE, "Food", null)).thenReturn(true);
        assertThatThrownBy(() -> service.create(userId, new CategoryRequest("Food", CategoryType.EXPENSE, null, null)))
                .isInstanceOf(ConflictException.class);
    }
}
