package com.example.expensetracker.transaction;

import com.example.expensetracker.transaction.dto.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(t.getId(), t.getType(), t.getAmount(), t.getCategory().getId(),
                t.getCategory().getName(), t.getNote(), t.getTransactionDate(), t.getCreatedAt());
    }
}
