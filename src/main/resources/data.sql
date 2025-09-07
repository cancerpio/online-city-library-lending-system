-- =========================
-- data.sql  (Initial Seed)
-- =========================

-- 1) 類型與規則：BOOK/JOURNAL
INSERT INTO book_categories (category, rule_max_concurrent, rule_loan_period_days)
VALUES
  ('BOOK',    5, 30),
  ('JOURNAL', 10, 30)
ON CONFLICT (category) DO NOTHING;

-- 2) 使用者：館員與一般會員
-- 密碼範例：
--   admin / admin123  -> $2a$10$1bVapA0u8BFOY5sJeW8DYuq/7K0m6.8fJqC6ej9IfDP8YtqS9kkOi
--   member1 / member123 -> $2a$10$HbR.2pXL37SHUThkn9KXFe7TQpyEZx.yP4Hfykka6zmy9vhrQcZ3i
INSERT INTO users (user_role, username, password_hash, is_active)
VALUES
  ('Librarian', 'admin',   '$2a$10$1bVapA0u8BFOY5sJeW8DYuq/7K0m6.8fJqC6ej9IfDP8YtqS9kkOi', TRUE),
  ('Member',    'member1', '$2a$10$HbR.2pXL37SHUThkn9KXFe7TQpyEZx.yP4Hfykka6zmy9vhrQcZ3i', TRUE)
ON CONFLICT DO NOTHING;

-- 3) 分館
INSERT INTO branches (branch_name)
VALUES
  ('Main Library'),
  ('East Branch'),
  ('Central'),
  ('West')
ON CONFLICT DO NOTHING;

-- 4) 書目（category_id 以子查詢由類型表取得，避免硬編 ID）
INSERT INTO books (unique_book_key, title, author, publish_year, category_id, extra)
VALUES
  ('effective java|joshua bloch|2018|1', 'Effective Java',        'Joshua Bloch',        2018, (SELECT id FROM book_categories WHERE category='BOOK'),    '{"isbn":"9780134685991"}'),
  ('clean code|robert c. martin|2008|1', 'Clean Code',            'Robert C. Martin',    2008, (SELECT id FROM book_categories WHERE category='BOOK'),    '{"isbn":"9780132350884"}'),
  ('nature journal vol. 1|editorial board|2023|2', 'Nature Journal Vol. 1', 'Editorial Board',     2023, (SELECT id FROM book_categories WHERE category='JOURNAL'), '{}'),
  ('spring in action|craig|2022|1', 'Spring in Action', 'Craig', 2022, (SELECT id FROM book_categories WHERE category='BOOK'), '{}'),
  ('java monthly|oracle|2024|1', 'Java Monthly', 'Oracle', 2024, (SELECT id FROM book_categories WHERE category='BOOK'), '{}'),
  -- 測試用書籍：BOOK 類型，11個副本用於測試 10本限制
  ('this is book|test author|2024|1', 'This is BOOK', 'Test Author', 2024, (SELECT id FROM book_categories WHERE category='BOOK'), '{"test":"true"}'),
  -- 測試用期刊：JOURNAL 類型，11個副本用於測試 5本限制
  ('this is journal|test editor|2024|1', 'This is JOURNAL', 'Test Editor', 2024, (SELECT id FROM book_categories WHERE category='JOURNAL'), '{"test":"true"}')
ON CONFLICT (unique_book_key) DO NOTHING;

-- 5) 實體副本（各館館藏）
INSERT INTO book_copies (book_id, branch_id, status, barcode)
VALUES
  ((SELECT id FROM books WHERE title='Effective Java'),        (SELECT id FROM branches WHERE branch_name='Main Library'), 'AVAILABLE', 'E001'),
  ((SELECT id FROM books WHERE title='Effective Java'),        (SELECT id FROM branches WHERE branch_name='East Branch'),  'AVAILABLE', 'E002'),
  ((SELECT id FROM books WHERE title='Clean Code'),            (SELECT id FROM branches WHERE branch_name='Main Library'), 'AVAILABLE', 'C001'),
  ((SELECT id FROM books WHERE title='Nature Journal Vol. 1'), (SELECT id FROM branches WHERE branch_name='Main Library'), 'AVAILABLE', 'N001'),
  -- DataSeeder 的數據
  ((SELECT id FROM books WHERE title='Spring in Action'),      (SELECT id FROM branches WHERE branch_name='Central'),     'AVAILABLE', 'B001'),
  ((SELECT id FROM books WHERE title='Spring in Action'),      (SELECT id FROM branches WHERE branch_name='Central'),     'AVAILABLE', 'B002'),
  ((SELECT id FROM books WHERE title='Spring in Action'),      (SELECT id FROM branches WHERE branch_name='Central'),     'AVAILABLE', 'B003'),
  ((SELECT id FROM books WHERE title='Spring in Action'),      (SELECT id FROM branches WHERE branch_name='West'),        'AVAILABLE', 'B004'),
  ((SELECT id FROM books WHERE title='Spring in Action'),      (SELECT id FROM branches WHERE branch_name='West'),        'AVAILABLE', 'B005'),
  ((SELECT id FROM books WHERE title='Java Monthly'),          (SELECT id FROM branches WHERE branch_name='Central'),     'AVAILABLE', 'M001'),
  ((SELECT id FROM books WHERE title='Java Monthly'),          (SELECT id FROM branches WHERE branch_name='Central'),     'AVAILABLE', 'M002'),
  ((SELECT id FROM books WHERE title='Java Monthly'),          (SELECT id FROM branches WHERE branch_name='Central'),     'AVAILABLE', 'M003'),
  ((SELECT id FROM books WHERE title='Java Monthly'),          (SELECT id FROM branches WHERE branch_name='Central'),     'AVAILABLE', 'M004'),
  ((SELECT id FROM books WHERE title='Java Monthly'),          (SELECT id FROM branches WHERE branch_name='Central'),     'AVAILABLE', 'M005'),
  
  -- 測試上線用的副本 
  -- BOOK 類型測試副本（11個副本，用於測試 10本限制）
  -- Copy IDs: 15-25 (This is BOOK)
  ((SELECT id FROM books WHERE title='This is BOOK'),          (SELECT id FROM branches WHERE branch_name='Main Library'), 'AVAILABLE', 'BOOK001'),
  ((SELECT id FROM books WHERE title='This is BOOK'),          (SELECT id FROM branches WHERE branch_name='Main Library'), 'AVAILABLE', 'BOOK002'),
  ((SELECT id FROM books WHERE title='This is BOOK'),          (SELECT id FROM branches WHERE branch_name='Main Library'), 'AVAILABLE', 'BOOK003'),
  ((SELECT id FROM books WHERE title='This is BOOK'),          (SELECT id FROM branches WHERE branch_name='Main Library'), 'AVAILABLE', 'BOOK004'),
  ((SELECT id FROM books WHERE title='This is BOOK'),          (SELECT id FROM branches WHERE branch_name='Main Library'), 'AVAILABLE', 'BOOK005'),
  ((SELECT id FROM books WHERE title='This is BOOK'),          (SELECT id FROM branches WHERE branch_name='East Branch'),  'AVAILABLE', 'BOOK006'),
  ((SELECT id FROM books WHERE title='This is BOOK'),          (SELECT id FROM branches WHERE branch_name='East Branch'),  'AVAILABLE', 'BOOK007'),
  ((SELECT id FROM books WHERE title='This is BOOK'),          (SELECT id FROM branches WHERE branch_name='Central'),     'AVAILABLE', 'BOOK008'),
  ((SELECT id FROM books WHERE title='This is BOOK'),          (SELECT id FROM branches WHERE branch_name='Central'),     'AVAILABLE', 'BOOK009'),
  ((SELECT id FROM books WHERE title='This is BOOK'),          (SELECT id FROM branches WHERE branch_name='West'),        'AVAILABLE', 'BOOK010'),
  ((SELECT id FROM books WHERE title='This is BOOK'),          (SELECT id FROM branches WHERE branch_name='West'),        'AVAILABLE', 'BOOK011'),
  
  -- JOURNAL 類型測試副本（11個副本，用於測試 5本限制）
  -- Copy IDs: 26-36 (This is JOURNAL)
  ((SELECT id FROM books WHERE title='This is JOURNAL'),       (SELECT id FROM branches WHERE branch_name='Main Library'), 'AVAILABLE', 'JOURNAL001'),
  ((SELECT id FROM books WHERE title='This is JOURNAL'),       (SELECT id FROM branches WHERE branch_name='Main Library'), 'AVAILABLE', 'JOURNAL002'),
  ((SELECT id FROM books WHERE title='This is JOURNAL'),       (SELECT id FROM branches WHERE branch_name='Main Library'), 'AVAILABLE', 'JOURNAL003'),
  ((SELECT id FROM books WHERE title='This is JOURNAL'),       (SELECT id FROM branches WHERE branch_name='Main Library'), 'AVAILABLE', 'JOURNAL004'),
  ((SELECT id FROM books WHERE title='This is JOURNAL'),       (SELECT id FROM branches WHERE branch_name='Main Library'), 'AVAILABLE', 'JOURNAL005'),
  ((SELECT id FROM books WHERE title='This is JOURNAL'),       (SELECT id FROM branches WHERE branch_name='East Branch'),  'AVAILABLE', 'JOURNAL006'),
  ((SELECT id FROM books WHERE title='This is JOURNAL'),       (SELECT id FROM branches WHERE branch_name='East Branch'),  'AVAILABLE', 'JOURNAL007'),
  ((SELECT id FROM books WHERE title='This is JOURNAL'),       (SELECT id FROM branches WHERE branch_name='Central'),     'AVAILABLE', 'JOURNAL008'),
  ((SELECT id FROM books WHERE title='This is JOURNAL'),       (SELECT id FROM branches WHERE branch_name='Central'),     'AVAILABLE', 'JOURNAL009'),
  ((SELECT id FROM books WHERE title='This is JOURNAL'),       (SELECT id FROM branches WHERE branch_name='West'),        'AVAILABLE', 'JOURNAL010'),
  ((SELECT id FROM books WHERE title='This is JOURNAL'),       (SELECT id FROM branches WHERE branch_name='West'),        'AVAILABLE', 'JOURNAL011')
ON CONFLICT (barcode) DO NOTHING;
