package com.example.expensetracker.category;

import com.example.expensetracker.budget.BudgetRepository;
import com.example.expensetracker.category.dto.CategoryRequest;
import com.example.expensetracker.category.dto.CategoryResponse;
import com.example.expensetracker.common.exception.ConflictException;
import com.example.expensetracker.common.exception.ForbiddenException;
import com.example.expensetracker.common.exception.ResourceNotFoundException;
import com.example.expensetracker.transaction.TransactionRepository;
import com.example.expensetracker.user.UserService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-C1..C2. */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final UserService userService;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository, BudgetRepository budgetRepository, UserService userService, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.userService = userService;
        this.categoryMapper = categoryMapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(UUID userId, CategoryType type) {
        return categoryRepository.findVisibleToUser(userId, type).stream().map(categoryMapper::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(UUID userId, CategoryRequest req) {
        String name = req.name().trim();
        ensureNameFree(userId, req.type(), name, null);
        Category category = new Category(userService.getReference(userId), name, req.type(), req.icon(), req.color());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID userId, UUID id, CategoryRequest req) {
        Category category = getOwned(userId, id);
        String name = req.name().trim();
        if (category.getType() != req.type() && transactionRepository.existsByCategoryId(id)) {
            throw new ConflictException("Cannot change the type of a category that has transactions");
        }
        ensureNameFree(userId, req.type(), name, id);
        category.setName(name);
        category.setType(req.type());
        category.setIcon(req.icon());
        category.setColor(req.color());
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Category category = getOwned(userId, id);
        if (transactionRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category has transactions and cannot be deleted");
        }
        if (budgetRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category has budgets and cannot be deleted");
        }
        categoryRepository.delete(category);
    }

    /** A category the user may USE: a system default or one of their own. Otherwise 404. */
    @Transactional(readOnly = true)
    public Category getUsable(UUID userId, UUID id) {
        return categoryRepository.findById(id)
                .filter(c -> c.isSystemDefault() || c.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    /** A category the user may CHANGE: only their own. Defaults → 403, others' → 404. */
    private Category getOwned(UUID userId, UUID id) {
        Category category = getUsable(userId, id);
        if (category.isSystemDefault()) {
            throw new ForbiddenException("System default categories are read-only");
        }
        return category;
    }

    private void ensureNameFree(UUID userId, CategoryType type, String name, UUID excludeId) {
        if (categoryRepository.existsVisibleByName(userId, type, name, excludeId)) {
            throw new ConflictException("A " + type + " category named '" + name + "' already exists");
        }
    }
}
