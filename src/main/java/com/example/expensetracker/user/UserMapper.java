package com.example.expensetracker.user;

import com.example.expensetracker.user.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getCurrencyCode(), user.getCreatedAt());
    }
}
