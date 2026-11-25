INSERT INTO categories (id, user_id, name, type, icon, color, created_at, updated_at)
VALUES
    (gen_random_uuid(), NULL, 'Food',          'EXPENSE', 'restaurant',     '#F44336', now(), now()),
    (gen_random_uuid(), NULL, 'Transport',     'EXPENSE', 'directions_bus', '#3F51B5', now(), now()),
    (gen_random_uuid(), NULL, 'Shopping',      'EXPENSE', 'shopping_bag',   '#E91E63', now(), now()),
    (gen_random_uuid(), NULL, 'Bills',         'EXPENSE', 'receipt',        '#FF9800', now(), now()),
    (gen_random_uuid(), NULL, 'Rent',          'EXPENSE', 'home',           '#795548', now(), now()),
    (gen_random_uuid(), NULL, 'Health',        'EXPENSE', 'local_hospital', '#009688', now(), now()),
    (gen_random_uuid(), NULL, 'Education',     'EXPENSE', 'school',         '#673AB7', now(), now()),
    (gen_random_uuid(), NULL, 'Entertainment', 'EXPENSE', 'movie',          '#9C27B0', now(), now()),
    (gen_random_uuid(), NULL, 'Other',         'EXPENSE', 'category',       '#607D8B', now(), now()),
    (gen_random_uuid(), NULL, 'Salary',        'INCOME',  'payments',       '#4CAF50', now(), now()),
    (gen_random_uuid(), NULL, 'Business',      'INCOME',  'store',          '#8BC34A', now(), now()),
    (gen_random_uuid(), NULL, 'Gift',          'INCOME',  'card_giftcard',  '#CDDC39', now(), now()),
    (gen_random_uuid(), NULL, 'Other',         'INCOME',  'category',       '#607D8B', now(), now());
