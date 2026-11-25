package com.example.expensetracker.transaction;

import com.example.expensetracker.category.Category;
import com.example.expensetracker.category.CategoryService;
import com.example.expensetracker.category.CategoryType;
import com.example.expensetracker.common.exception.BadRequestException;
import com.example.expensetracker.common.exception.ResourceNotFoundException;
import com.example.expensetracker.common.response.PageResponse;
import com.example.expensetracker.transaction.dto.CreateTransactionRequest;
import com.example.expensetracker.transaction.dto.TransactionFilter;
import com.example.expensetracker.transaction.dto.TransactionResponse;
import com.example.expensetracker.transaction.dto.UpdateTransactionRequest;
import com.example.expensetracker.user.UserService;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-T1..T4. */
@Service
public class TransactionService {

    static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("createdAt"));
    private static final Set<String> SORTABLE = Set.of("transactionDate", "amount", "createdAt", "type");

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final UserService userService;
    private final TransactionMapper transactionMapper;

    public TransactionService(TransactionRepository transactionRepository, CategoryService categoryService, UserService userService, TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
        this.userService = userService;
        this.transactionMapper = transactionMapper;
    }

    @Transactional
    public TransactionResponse create(UUID userId, CreateTransactionRequest req) {
        Category category = usableCategoryMatching(userId, req.categoryId(), req.type());
        Transaction tx = new Transaction(userService.getReference(userId), category, req.type(), req.amount(),
                blankToNull(req.note()), req.transactionDate());
        return transactionMapper.toResponse(transactionRepository.save(tx));
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID userId, UUID id) {
        return transactionMapper.toResponse(load(userId, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> list(UUID userId, TransactionFilter filter, Pageable pageable) {
        validate(filter);
        Pageable safe = withSafeSort(pageable);
        return PageResponse.of(
                transactionRepository.findAll(TransactionSpecifications.forUser(userId, filter), safe),
                transactionMapper::toResponse);
    }

    @Transactional
    public TransactionResponse update(UUID userId, UUID id, UpdateTransactionRequest req) {
        Transaction tx = load(userId, id);
        tx.setCategory(usableCategoryMatching(userId, req.categoryId(), req.type()));
        tx.setType(req.type());
        tx.setAmount(req.amount());
        tx.setNote(blankToNull(req.note()));
        tx.setTransactionDate(req.transactionDate());
        return transactionMapper.toResponse(transactionRepository.saveAndFlush(tx));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        transactionRepository.delete(load(userId, id));
    }

    private Transaction load(UUID userId, UUID id) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    private Category usableCategoryMatching(UUID userId, UUID categoryId, CategoryType type) {
        Category category = categoryService.getUsable(userId, categoryId);
        if (category.getType() != type) {
            throw new BadRequestException("Transaction type " + type + " does not match category type "
                    + category.getType());
        }
        return category;
    }

    private static void validate(TransactionFilter f) {
        if (f == null) {
            return;
        }
        if (f.from() != null && f.to() != null && f.from().isAfter(f.to())) {
            throw new BadRequestException("'from' must be on or before 'to'");
        }
        if (f.minAmount() != null && f.maxAmount() != null && f.minAmount().compareTo(f.maxAmount()) > 0) {
            throw new BadRequestException("'minAmount' must be less than or equal to 'maxAmount'");
        }
    }

    /** Applies the default sort when none is given and rejects unknown sort fields. */
    private static Pageable withSafeSort(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
        }
        for (Sort.Order order : sort) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE);
            }
        }
        // Tie-breaker keeps paging stable
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                sort.and(Sort.by(Sort.Order.desc("createdAt"))));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
