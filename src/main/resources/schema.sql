-- =========================================
-- schema.sql  (DDL + Indexes & Constraints)
-- =========================================

-- === USERS（使用者）===
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,                                         -- 使用者 ID
    user_role     TEXT NOT NULL CHECK (user_role IN ('Librarian','Member')),     -- 角色（本題假設一人一角）
    username      TEXT NOT NULL,                                                 -- 使用者帳號（另建不分大小寫唯一索引）
    password_hash TEXT NOT NULL,                                                 -- 密碼雜湊
    is_active     BOOLEAN DEFAULT TRUE,                                          -- 是否啟用
    created_at    TIMESTAMPTZ DEFAULT now(),                                     -- 建立時間
    updated_at    TIMESTAMPTZ DEFAULT now()                                      -- 更新時間
);

-- 不分大小寫唯一（避免 Tom/tom 兩個帳號）
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_username_ci ON users (LOWER(username));



-- === BOOK CATEGORIES（書籍類型 + 借閱規則）===
CREATE TABLE IF NOT EXISTS book_categories (
    id                    BIGSERIAL PRIMARY KEY,                                 -- 類型 ID
    category              TEXT NOT NULL UNIQUE,                                  -- 類型描述（BOOK/JOURNAL 等）
    rule_max_concurrent   INT  NOT NULL,                                         -- 該類型同時可借上限
    rule_loan_period_days INT  NOT NULL                                          -- 借閱期限（天）
);

-- 建立預設類別, 當有某個類別被刪掉時，該類別的書都被設定為'Uncategorized'
INSERT INTO book_categories (category, rule_max_concurrent, rule_loan_period_days)
VALUES ('Uncategorized', 5, 30)
    ON CONFLICT (category) DO NOTHING;


-- === BOOKS（書目，不含副本）===
CREATE TABLE IF NOT EXISTS books (
    id           BIGSERIAL PRIMARY KEY,                                          -- 書籍 ID
    unique_book_key      TEXT NOT NULL,                                          -- trim / lowercase後的唯一識別(書名+作者+出版年份+category_id)，避免大小寫和一些不當的空白造成誤判重複，
    title        TEXT NOT NULL,                                                  -- 書名
    author       TEXT NOT NULL,                                                  -- 作者
    publish_year INT,                                                            -- 出版年份
    category_id  BIGINT NOT NULL  DEFAULT 1 REFERENCES book_categories(id) ON DELETE SET DEFAULT,  -- 類型 FK, 預設為 1: 'Uncategorized'
    extra        TEXT DEFAULT ''                                               -- 延伸資訊（ISBN/tags…）
);
ALTER TABLE books
    ADD CONSTRAINT ux_books_unique_book_key UNIQUE (unique_book_key);

-- 搜尋索引
CREATE INDEX IF NOT EXISTS idx_books_title_lower  ON books (LOWER(title));
CREATE INDEX IF NOT EXISTS idx_books_author_lower ON books (LOWER(author));
CREATE INDEX IF NOT EXISTS idx_books_year         ON books (publish_year);



-- === BRANCHES（分館）===
CREATE TABLE IF NOT EXISTS branches (
    id          BIGSERIAL PRIMARY KEY,                                           -- 分館 ID
    branch_name TEXT NOT NULL                                                    -- 分館名
);

-- 分館名唯一（避免重複）
ALTER TABLE branches
    ADD CONSTRAINT ux_branches_name UNIQUE (branch_name);



-- === BOOK COPIES（實體副本/館藏）===
CREATE TABLE IF NOT EXISTS book_copies (
    id         BIGSERIAL PRIMARY KEY,                                            -- 副本 ID
    book_id    BIGINT NOT NULL REFERENCES books(id),                             -- 書目 FK
    branch_id  BIGINT NOT NULL REFERENCES branches(id),                          -- 所屬分館 FK
    status     TEXT NOT NULL CHECK (status IN ('AVAILABLE','BORROWED','LOST','MAINTENANCE','DELETED')), -- 副本狀態
    barcode    TEXT NOT NULL UNIQUE,                                             -- 條碼字串，保留前導 0，故用 TEXT
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 彙總/查詢常用索引（依 書/分館/狀態）
CREATE INDEX IF NOT EXISTS idx_copies_bbs ON book_copies (book_id, branch_id, status);



-- === LOANS（借閱紀錄）===
CREATE TABLE IF NOT EXISTS loans (
    id               BIGSERIAL PRIMARY KEY,                                      -- 借閱紀錄 ID
    copy_id          BIGINT NOT NULL REFERENCES book_copies(id),                 -- 借出的副本
    borrowed_user_id BIGINT NOT NULL REFERENCES users(id),                       -- 借用人
    borrowed_at      TIMESTAMPTZ DEFAULT now(),                                  -- 借出時間
    due_date         TIMESTAMPTZ NOT NULL,                                       -- 到期時間（由應用依規則計算）
    returned_at      TIMESTAMPTZ DEFAULT NULL,                                   -- 歸還時間（NULL=未還）
    created_at       TIMESTAMPTZ DEFAULT now(),
    updated_at       TIMESTAMPTZ DEFAULT now()
);

-- 關鍵一致性：同一副本「同一時間」只能有一筆未歸還
CREATE UNIQUE INDEX IF NOT EXISTS unique_active_loan_per_copy
  ON loans(copy_id)
  WHERE returned_at IS NULL;

-- 到期時間須晚於借出時間, 還書時間須晚於借出時間。僅避免間混亂跟還書規則無關
ALTER TABLE loans
  ADD CONSTRAINT chk_return_after_borrow
  CHECK (returned_at IS NULL OR returned_at >= borrowed_at);
ALTER TABLE loans
  ADD CONSTRAINT chk_due_after_borrow
  CHECK (due_date > borrowed_at);

-- 常用查詢索引：查使用者當前借閱 / 到期清單
CREATE INDEX IF NOT EXISTS idx_loans_user_active ON loans (borrowed_user_id, returned_at);
CREATE INDEX IF NOT EXISTS idx_loans_due_date    ON loans (due_date);



-- === EXTERNAL VERIFICATIONS（外部驗證稽核，純記錄）===
CREATE TABLE IF NOT EXISTS external_verifications (
    id                   BIGSERIAL PRIMARY KEY,                                   -- 稽核紀錄 ID
    employee_no          TEXT DEFAULT NULL,                                       -- 員工編號（不存 token）
    verifications_time   TIMESTAMPTZ DEFAULT now(),                               -- 呼叫時間
    verifications_result BOOLEAN DEFAULT FALSE                                    -- 結果
    -- 如需更完整稽核，可另加 http_status/message/performed_by_user_id 等欄位
);
