package com.example.expensetracker;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.expensetracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class ExpenseTrackerApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoadsMigrationsApplyAndDefaultsAreSeeded() {
        Integer defaults = jdbc.queryForObject("select count(*) from categories where user_id is null", Integer.class);
        assertThat(defaults).isEqualTo(13);
    }
}
