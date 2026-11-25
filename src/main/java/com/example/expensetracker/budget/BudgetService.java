package com.example.expensetracker.budget;

import com.example.expensetracker.budget.dto.BudgetRequest;
import com.example.expensetracker.budget.dto.BudgetResponse;
import com.example.expensetracker.category.Category;
import com.example.expensetracker.category.CategoryService;
import com.example.expensetracker.category.CategoryType;
import com.example.expensetracker.common.exception.BadRequestException;
import com.example.expensetracker.common.exception.ConflictException;
import com.example.expensetracker.common.exception.ResourceNotFoundException;
import com.example.expensetracker.transaction.TransactionRepository;
import com.example.expensetracker.transaction.TransactionRepository.CategoryTotal;
import com.example.expensetracker.user.UserService;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-B1..B3. spent = sum of EXPENSE transactions in the budget's month. */
@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final UserService userService;
    private final BudgetMapper budgetMapper;

    public BudgetService(BudgetRepository budgetRepository, TransactionRepository transactionRepository, CategoryService categoryService, UserService userService, BudgetMapper budgetMapper) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
        this.userService = userService;
        this.budgetMapper = budgetMapper;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(UUID userId, int year, int month) {
        List<Budget> budgets = budgetRepository.findByUserIdAndYearAndMonth(userId, year, month);
        if (budgets.isEmpty()) {
            return List.of();
        }
        SpentLookup spent = spentFor(userId, YearMonth.of(year, month));
        return budgets.stream()
                .map(b -> budgetMapper.toResponse(b, spent.of(b)))
                // overall budget first, then by category name
                .sorted(Comparator.comparing((BudgetResponse r) -> r.categoryId() != null)
                        .thenComparing(r -> r.categoryName() == null ? "" : r.categoryName()))
                .toList();
    }

    @Transactional
    public BudgetResponse create(UUID userId, BudgetRequest req) {
        Category category = expenseCategoryOrNull(userId, req.categoryId());
        ensurePeriodFree(userId, req, null);
        Budget budget = budgetRepository.save(new Budget(userService.getReference(userId), category, req.amount(),
                req.year(), req.month()));
        return withSpent(userId, budget);
    }

    @Transactional
    public BudgetResponse update(UUID userId, UUID id, BudgetRequest req) {
        Budget budget = load(userId, id);
        Category category = expenseCategoryOrNull(userId, req.categoryId());
        ensurePeriodFree(userId, req, id);
        budget.setCategory(category);
        budget.setAmount(req.amount());
        budget.setYear(req.year());
        budget.setMonth(req.month());
        return withSpent(userId, budgetRepository.saveAndFlush(budget));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        budgetRepository.delete(load(userId, id));
    }

    private BudgetResponse withSpent(UUID userId, Budget budget) {
        return budgetMapper.toResponse(budget,
                spentFor(userId, YearMonth.of(budget.getYear(), budget.getMonth())).of(budget));
    }

    private SpentLookup spentFor(UUID userId, YearMonth ym) {
        List<CategoryTotal> totals = transactionRepository.sumByCategory(userId, CategoryType.EXPENSE,
                ym.atDay(1), ym.atEndOfMonth());
        Map<UUID, BigDecimal> byCategory = totals.stream()
                .collect(Collectors.toMap(CategoryTotal::getCategoryId, CategoryTotal::getTotal));
        BigDecimal overall = byCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SpentLookup(byCategory, overall);
    }

    private Budget load(UUID userId, UUID id) {
        return budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
    }

    private Category expenseCategoryOrNull(UUID userId, UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryService.getUsable(userId, categoryId);
        if (category.getType() != CategoryType.EXPENSE) {
            throw new BadRequestException("Budgets can only be set for EXPENSE categories");
        }
        return category;
    }

    private void ensurePeriodFree(UUID userId, BudgetRequest req, UUID excludeId) {
        if (budgetRepository.existsForPeriod(userId, req.categoryId(), req.year(), req.month(), excludeId)) {
            throw new ConflictException("A budget for this category and month already exists");
        }
    }

    private record SpentLookup(Map<UUID, BigDecimal> byCategory, BigDecimal overall) {
        BigDecimal of(Budget b) {
            return b.getCategory() == null
                    ? overall
                    : byCategory.getOrDefault(b.getCategory().getId(), BigDecimal.ZERO);
        }
    }
}
