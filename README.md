# 線上圖書借閱系統 - Online City Library Lending System

## Quick Start

### 前置需求
- Docker & Docker Compose
- Java 21+ (僅開發需要)

### 使用JWT Token 做身份驗證
- 使用帳號密碼登入，或者註冊成功後，API會回應JWT Token
- 後續需要在header帶上JWT Token才能使用其他API的功能

### 啟動 Production 環境
- 正式環境中，資料庫會使用雲端託管(Ex: AWS RDS)，或獨立維運的服務，不會與本系統一起由 docker-compose 啟動。
- 本專案的docker-compose-prd.yml僅供模擬Production環境的用途( DB / 館員驗證的外部API )。
```bash
# 1. 複製環境變數範例檔案
cp .env.prod.example .env.prod

# 2. 編輯正式環境配置 (已包含預設的可用設定)
# vim .env.prod

# 3. 啟動模擬正式環境 (PostgreSQL + Wiremock + Build 主程式)
docker-compose -f docker-compose-prd.yml --env-file .env.prod up -d --build

# 4. 檢查docker狀態
docker-compose -f docker-compose-prd.yml --env-file .env.prod ps

# 5. 重要：使用 API 前必須先註冊取得 JWT token
# 所有 API 都需要 JWT 認證，請先執行以下步驟取得 token：
# 6.1 註冊一般用戶
curl -X POST http://localhost:8888/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testmember1","password":"password123","role":"Member"}'

# 6.2 登入取得 JWT token (從API Response中複製 JWT token字串)
curl -X POST http://localhost:8888/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testmember1","password":"password123"}'

# 6.3 使用 JWT token 測試 API (將 YOUR_JWT_TOKEN 替換為實際 token)
# curl -H "Authorization: Bearer YOUR_JWT_TOKEN" http://localhost:8888/api/books/search
```

### 啟動 Dev 環境
```bash
# 啟動開發環境 (Testcontainers + Mock API)
docker-compose -f docker-compose-dev.yml up -d

# 檢查docker狀態
docker-compose -f docker-compose-dev.yml ps

```

### 停止環境
```bash
# 停止 Production 環境
docker-compose -f docker-compose-prd.yml --env-file .env.prod down

# 停止 Dev 環境
docker-compose -f docker-compose-dev.yml down
```

## 整合測試

### 測試環境準備
```bash
# 1. 啟動環境
docker-compose -f docker-compose-prd.yml --env-file .env.prod up -d --build

# 2. 確認container狀態
docker-compose -f docker-compose-prd.yml --env-file .env.prod ps

# 3. 確認API Server log
docker logs -f library-app
```

### 測試方式

#### 1.curl
以下提供完整的 curl 命令進行 API 測試，適合快速驗證功能。

#### 2.Postman (Import 專案根目錄下的postman-collection.json)
也可以使用 Postman 進行測試，建議設定以下 Collection 變數：
- `base_url`: `http://localhost:8888` (API 基礎網址)
- `jwt_token`: JWT 認證 token (可動態切換不同使用者)

使用方式：
1. 先執行 Step 1 取得 JWT token
2. 在 Postman Collection 中設定 `jwt_token` 變數
3. 在 API 請求的 Authorization Header 中使用 `Bearer {{jwt_token}}`
4. 可透過修改 `jwt_token` 變數快速切換不同使用者進行測試

### 測試步驟

#### Step 1: 會員管理測試
```bash
# 1.1 測試未註冊用戶登入（應該失敗）
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/auth/login \
  -X POST -H "Content-Type: application/json" \
  -d '{"username":"memb","password":"memb12345","role":"Member"}'
# 預期結果: 401 UNAUTHORIZED

curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/auth/login \
  -X POST -H "Content-Type: application/json" \
  -d '{"username":"lib","password":"lib12345","role":"Librarian"}'
# 預期結果: 401 UNAUTHORIZED

# 1.2 註冊用戶（使用符合長度要求的密碼）
 ## 館員
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/auth/register \
  -X POST -H "Content-Type: application/json" \
  -d '{"username":"libuser","password":"lib12345","role":"Librarian"}'
# 預期結果: 201 Created, 返回 JWT token

curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/auth/register \
 ## 一般使用者
  -X POST -H "Content-Type: application/json" \
  -d '{"username":"membuser","password":"memb12345","role":"Member"}'
# 預期結果: 201 Created, 返回 JWT token

# 1.3 測試登入
 ## 館員
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/auth/login \
  -X POST -H "Content-Type: application/json" \
  -d '{"username":"libuser","password":"lib12345","role":"Librarian"}'
# 預期結果: 200 OK, 返回 JWT token
 ## 一般使用者
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/auth/login \
  -X POST -H "Content-Type: application/json" \
  -d '{"username":"membuser","password":"memb12345","role":"Member"}'
# 預期結果: 200 OK, 返回 JWT token

# 設定JWT tokens (從上述回應中複製實際的token值)
export LIB_TOKEN="your_lib_jwt_token_here"
export MEMB_TOKEN="your_memb_jwt_token_here"
```

#### Step 2: 書籍管理測試 (一般使用者)
```bash
# 2.1 測試一般用戶新增書籍（應該失敗）
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/books \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MEMB_TOKEN" \
  -d '{"title":"Test Book","author":"Test Author","publishYear":2024,"categoryId":2,"extra":"{}","bookCopies":[{"branchId":1,"quantity":1}]}'
# 預期結果: 403 Forbidden

# 2.2 測試一般用戶搜尋書籍
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/books/search \
  -H "Authorization: Bearer $MEMB_TOKEN"
# 預期結果: 200 OK, 返回書籍列表

# 2.3 測試按標題搜尋
curl -s -w "\nHTTP Status: %{http_code}\n" "http://localhost:8888/api/books/search?title=Spring" \
  -H "Authorization: Bearer $MEMB_TOKEN"
# 預期結果: 200 OK, 返回相關書籍

# 2.4 測試按作者搜尋
curl -s -w "\nHTTP Status: %{http_code}\n" "http://localhost:8888/api/books/search?author=Joshua" \
  -H "Authorization: Bearer $MEMB_TOKEN"
# 預期結果: 200 OK, 返回相關書籍

# 2.5 測試按年份搜尋
curl -s -w "\nHTTP Status: %{http_code}\n" "http://localhost:8888/api/books/search?year=2024" \
  -H "Authorization: Bearer $MEMB_TOKEN"
# 預期結果: 200 OK, 返回相關書籍
```

#### Step 3: 書籍管理測試 (館員)
```bash
# 3.1 測試館員新增書籍
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/books \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LIB_TOKEN" \
  -d '{"title":"Test Book by Librarian","author":"Librarian Author","publishYear":2024,"categoryId":2,"extra":"{}","bookCopies":[{"branchId":1,"quantity":2}]}'
# 預期結果: 200 OK, 返回書籍 ID

# 3.2 測試館員搜尋書籍（與一般用戶相同）
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/books/search \
  -H "Authorization: Bearer $LIB_TOKEN"
# 預期結果: 200 OK, 返回書籍列表
```

#### Step 4: 分館管理測試 (一般使用者)
```bash
# 4.1 測試一般用戶新增分館（應該失敗）
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/branches \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MEMB_TOKEN" \
  -d '{"name":"Test Branch"}'
# 預期結果: 403 Forbidden

# 4.2 測試一般用戶查看分館
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/branches \
  -H "Authorization: Bearer $MEMB_TOKEN"
# 預期結果: 200 OK, 返回分館列表
```

#### Step 5: 分館管理測試 (館員)
```bash
# 5.1 測試館員新增分館
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/branches \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LIB_TOKEN" \
  -d '{"name":"Test Branch by Librarian"}'
# 預期結果: 200 OK, 返回分館 ID

# 5.2 測試館員查看分館
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/branches \
  -H "Authorization: Bearer $LIB_TOKEN"
# 預期結果: 200 OK, 返回分館列表
```

#### Step 6: 借閱與還書測試 (一般使用者)
```bash
# 6.1 測試一般用戶借書
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/loans/borrow \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MEMB_TOKEN" \
  -d '{"items":[{"bookCopiesId":5},{"bookCopiesId":6}]}'
# 預期結果: 201 Created, 返回借閱記錄

# 6.2 測試一般用戶還書
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/loans/return \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MEMB_TOKEN" \
  -d '{"items":[{"bookCopiesId":5},{"bookCopiesId":6}]}'
# 預期結果: 200 OK, 返回還書記錄
```

#### Step 7: 借閱與還書測試 (館員)
```bash
# 7.1 測試館員借書
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/loans/borrow \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LIB_TOKEN" \
  -d '{"items":[{"bookCopiesId":5},{"bookCopiesId":6}]}'
# 預期結果: 201 Created, 返回借閱記錄

# 7.2 測試館員還書
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/loans/return \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LIB_TOKEN" \
  -d '{"items":[{"bookCopiesId":5},{"bookCopiesId":6}]}'
# 預期結果: 200 OK, 返回還書記錄
```

#### Step 8: 借閱已經被借走的書
```bash
# 8.1 memb 用戶借書 (預期成功)
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/loans/borrow \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MEMB_TOKEN" \
  -d '{"items":[{"bookCopiesId":5},{"bookCopiesId":6}]}'
# 預期結果: 201 Created

# 8.2 lib 用戶嘗試借已被借走的書 (預期失敗)
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/loans/borrow \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LIB_TOKEN" \
  -d '{"items":[{"bookCopiesId":5},{"bookCopiesId":6}]}'
# 預期結果: 409 Conflict, 已被另一個帳號借走

# 8.3 memb 用戶還書 (預期成功)
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/loans/return \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MEMB_TOKEN" \
  -d '{"items":[{"bookCopiesId":5},{"bookCopiesId":6}]}'
# 預期結果: 200 OK

# 8.4 lib 用戶借書 (預期成功)
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/loans/borrow \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LIB_TOKEN" \
  -d '{"items":[{"bookCopiesId":5},{"bookCopiesId":6}]}'
# 預期結果: 201 Created
```

#### Step 9: 歸還沒有借的書
```bash
# 9.1 lib 用戶嘗試歸還沒有借的書 (預期失敗)
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/loans/return \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LIB_TOKEN" \
  -d '{"items":[{"bookCopiesId":1}]}'
# 預期結果: 409 Conflict, 未借閱的書無法歸還
```

#### Step 10: 借閱圖書(categories=2 BOOK): 10本限制測試
```bash
# 10.1 使用memb用戶借10本BOOK類型書籍 (預期成功)
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/loans/borrow \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MEMB_TOKEN" \
  -d '{"items":[{"bookCopiesId":15},{"bookCopiesId":16},{"bookCopiesId":17},{"bookCopiesId":18},{"bookCopiesId":19},{"bookCopiesId":20},{"bookCopiesId":21},{"bookCopiesId":22},{"bookCopiesId":23},{"bookCopiesId":24}]}'
# 預期結果: 201 Created

# 10.2 嘗試借第11本BOOK類型書籍 (預期失敗 - 超過限制)
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/loans/borrow \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MEMB_TOKEN" \
  -d '{"items":[{"bookCopiesId":25}]}'
# 預期結果: 422 Unprocessable Entity, BOOK limit exceeded
```

#### Step 11: 借閱書籍(categories=3 JOURNAL): 5本限制測試
```bash
# 11.1 使用memb用戶借5本JOURNAL類型書籍 (預期成功)
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/loans/borrow \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MEMB_TOKEN" \
  -d '{"items":[{"bookCopiesId":26},{"bookCopiesId":27},{"bookCopiesId":28},{"bookCopiesId":29},{"bookCopiesId":30}]}'
# 預期結果: 201 Created

# 11.2 嘗試借第6本JOURNAL類型書籍 (預期失敗 - 超過限制)
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/loans/borrow \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MEMB_TOKEN" \
  -d '{"items":[{"bookCopiesId":31}]}'
# 預期結果: 422 Unprocessable Entity, JOURNAL limit exceeded
```

#### Step 12: 新增書籍 (一般使用者)
```bash
# 12.1 memb用戶嘗試新增書籍 (預期失敗 - 權限不足)
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/books \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MEMB_TOKEN" \
  -d '{"title":"newBook","author":"Test Author","publishYear":2024,"categoryId":2,"extra":"{}","bookCopies":[{"branchId":1,"quantity":1}]}'
# 預期結果: 403 Forbidden
```

#### Step 13: 新增書籍 (管理員)
```bash
# 13.1 lib用戶新增書籍 (預期成功)
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/books \
  -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LIB_TOKEN" \
  -d '{"title":"newBook","author":"Test Author","publishYear":2024,"categoryId":2,"extra":"{}","bookCopies":[{"branchId":1,"quantity":1}]}'
# 預期結果: 200 OK, 返回書籍 ID
```

#### Step 14: 修改書籍 (一般使用者)
```bash
# 14.1 memb用戶嘗試修改書籍 (預期失敗 - 權限不足)
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/books/1 \
  -X PATCH -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MEMB_TOKEN" \
  -d '{"publishYear":2020}'
# 預期結果: 403 Forbidden
```

#### Step 15: 修改書籍 (管理員)
```bash
# 15.1 lib用戶修改書籍 (預期成功)
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/books/1 \
  -X PATCH -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LIB_TOKEN" \
  -d '{"publishYear":2020}'
# 預期結果: 200 OK, 返回 {"updated":true,"id":10}
```

#### Step 16: 搜尋書籍 (一般使用者)
```bash
# 16.1 memb用戶搜尋newBook (預期成功)
curl -s -w "\nHTTP Status: %{http_code}\n" "http://localhost:8888/api/books/search?title=newbook" \
  -H "Authorization: Bearer $MEMB_TOKEN"
# 預期結果: 200 OK, 有找到剛剛館員新增的書
```

#### Step 17: 搜尋書籍 (館員)
```bash
# 17.1 lib用戶搜尋newBook (預期成功)
curl -s -w "\nHTTP Status: %{http_code}\n" "http://localhost:8888/api/books/search?title=newbook" \
  -H "Authorization: Bearer $LIB_TOKEN"
# 預期結果: 200 OK, 有找到剛剛館員新增的書
```

#### Step 18: 刪除書籍副本 (一般使用者)
```bash
# 18.1 memb用戶嘗試刪除書籍副本 (預期失敗 - 權限不足)
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/books/copies/1 \
  -X DELETE -H "Authorization: Bearer $MEMB_TOKEN"
# 預期結果: 403 Forbidden
```

#### Step 19: 刪除書籍副本 (管理員)
```bash
# 19.1 lib用戶刪除書籍副本 (預期成功)
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8888/api/books/copies/1 \
  -X DELETE -H "Authorization: Bearer $LIB_TOKEN"
# 預期結果: 200 OK, 返回 {"bookId":9,"copyId":37,"message":"Book copy deleted successfully","branchId":1}
```

#### Step 20: 5日前通知
```bash
# 20.1 檢查Docker logs中的逾期檢查日誌
docker logs library-app | grep "逾期檢查" | tail -5
# 預期結果: 看到定期的逾期檢查日誌，如 "[2025-09-07 09:22:56] 逾期檢查完成 - 無需要通知的書籍"
```

### 測試完成後清理
```bash
# 停止服務
docker-compose -f docker-compose-prd.yml --env-file .env.prod down
```

## 環境變數設定

### 正式環境
- 使用 `.env.prod` 檔案管理敏感資訊
- 包含 JWT secret、資料庫密碼、館員驗證用外部API設定等

### 開發環境
- 無需額外配置，使用 Testcontainers 自動管理
- 保持簡單性，專注於開發效率

### 逾期通知
系統會自動檢查逾期書籍並用System.out.print模擬通知：

- **檢查頻率**：可透過環境變數 `OVERDUE_CHECK_INTERVAL` 設定（單位：分鐘）
- **預設值**：1分鐘檢查一次
- **通知條件**：借閱超過5天的書籍
- **通知方式**：在應用程式日誌中記錄逾期通知 (docker logs library-app | grep "逾期檢查")

#### 可調整的環境變數
在 `.env.prod` 檔案中可以設定以下變數：

```bash
# 逾期檢查間隔（分鐘）
OVERDUE_CHECK_INTERVAL=5

# JWT 設定
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400

# 資料庫設定
POSTGRES_PASSWORD=your-db-password
POSTGRES_USER=postgres
POSTGRES_DB=library

# 外部 API 設定
LIBRARY_EXTERNAL_API_URL=https://your-api.com
LIBRARY_EXTERNAL_API_AUTH_HEADER=your-auth-header
```

## 環境對比

| 環境 | 端口 | 數據庫 | 外部 API | 容器數量 | 配置方式 | 用途 |
|------|------|--------|----------|----------|----------|------|
| **Production** | 8888 | 外部 PostgreSQL | Wiremock 模擬 | 3個 | `.env.prod` | 正式環境模擬 |
| **Dev** | 8889 | Testcontainers | Mock 服務 | 1個 | 無需配置 | 快速開發測試 |

## 本地開發 (開發者專用)

### Build專案
```bash
# Build & 打包
./mvnw clean package or mvn clean package

# 執行測試
./mvnw test

# 執行
java -jar target/online-city-library-lending-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 使用Docker重新Build並啟動程式
```bash
# 修改Code後重新Build，並執行
docker-compose -f docker-compose-dev.yml up --build -d
```

## API 端點說明

### 認證相關

#### 1. 使用者註冊
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123",
  "email": "test@example.com",
  "role": "Member"
}
```

**密碼要求：**
- 長度：8 ~ 12
- 內容：任意字串

**驗證錯誤範例：**
```json
{
  "details": [
    {
      "message": "Password must be between 8 and 12 characters",
      "field": "password"
    }
  ],
  "error": "Validation failed"
}
```

**Response:**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "role": "Member",
  "createdAt": "2025-09-05T15:30:00Z"
}
```

#### 2. 館員註冊 (需要外部 API 驗證)
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "librarian",
  "password": "password123",
  "email": "librarian@example.com",
  "role": "Librarian"
}
```

**Response:**
```json
{
  "id": 2,
  "username": "librarian",
  "email": "librarian@example.com",
  "role": "Librarian",
  "createdAt": "2025-09-05T15:30:00Z"
}
```

#### 3. 會員登入
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "expiresIn": 3600
}
```

### 書籍管理

#### 4. 新增書籍 (僅館員)
```http
POST /api/books
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Java 程式設計",
  "author": "林信良",
  "publishYear": 2023,
  "categoryId": 1,
  "extra": "{}",
  "bookCopies": [
    {
      "branchId": 1,
      "quantity": 5
    }
  ]
}
```

**Response:**
```json
{
  "id": 1
}
```

#### 5. 更新書籍 (僅館員)
```http
PATCH /api/books/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "更新的書名",
  "author": "更新的作者",
  "publishYear": 2024,
  "categoryId": 2
}
```

**說明：**
- 只更新request body指定的欄位，其他欄位保持原值 

**Response:**
```json
{
  "id": 1,
  "updated": true
}
```

#### 6. 刪除書籍副本 (僅館員)
```http
DELETE /api/books/copies/{copyId}
Authorization: Bearer <token>
```

**說明：**
- 軟刪除：將副本狀態標記為 `DELETED`
- 不會實際刪除資料，保護借閱記錄完整性
- 無法刪除已被借出的副本

**Response:**
```json
{
  "message": "Book copy deleted successfully",
  "copyId": 1,
  "bookId": 1,
  "branchId": 1
}
```

#### 7. 搜尋書籍 (所有用戶都可使用)
```http
GET /api/books/search?title=Java&author=林信良&year=2023&page=1&size=10
Authorization: Bearer <token>
```

**Response:**
```json
{
  "books": [
    {
      "bookId": 1,
      "title": "Java 程式設計",
      "author": "林信良",
      "publishYear": 2023,
      "categoryId": 1,
      "extra": "{}",
      "branches": [
        {
          "branchId": 1,
          "branchName": "Main Library",
          "total": 5,
          "available": 3
    }
  ]
}
```

### 借閱與還書

#### 6. 借書
```http
POST /api/loans/borrow
Authorization: Bearer <token>
Content-Type: application/json

{
  "items": [
    {"bookCopiesId": 1},
    {"bookCopiesId": 2}
  ]
}
```

**可用的 book_copies.id：**
- **BOOK 類型** (限制 10 本)：ID 1-4, 5-14, 15-25
- **JOURNAL 類型** (限制 5 本)：ID 26-36

**Response:**
```json
{
  "loans": [
    {
      "loanId": 1,
      "copyId": 1,
      "dueAt": "2025-10-05T15:30:00Z"
    }
  ]
}
```

#### 7. 還書
```http
POST /api/loans/return
Authorization: Bearer <token>
Content-Type: application/json

{
  "items": [
    {"bookCopiesId": 1},
    {"bookCopiesId": 2}
  ]
}
```

**可用的 book_copies.id：**
- **BOOK 類型** (限制 10 本)：ID 1-4, 5-14, 15-25
- **JOURNAL 類型** (限制 5 本)：ID 26-36

**Response:**
```json
{
  "loans": [
    {
      "loanId": 1,
      "copyId": 1,
      "returnedAt": "2025-09-10T10:00:00Z"
    }
  ]
}
```

#### 8. 查看借閱記錄
```http
GET /api/borrows/my-borrows
Authorization: Bearer <token>
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "bookTitle": "Java 程式設計",
      "libraryName": "中央圖書館",
      "borrowDate": "2025-09-05T15:30:00Z",
      "dueDate": "2025-10-05T15:30:00Z",
      "status": "BORROWED"
    }
  ],
  "totalElements": 1
}
```

## 登入說明

### 獲取 JWT Token
1. 先註冊用戶或館員
2. 使用 `/api/auth/login` 獲取 JWT token
3. 在後續請求的 Header 中加入：`Authorization: Bearer <token>`

### 角色權限
- **Member**: 可借書、還書、搜尋書籍
- **Librarian**: 可新增書籍、管理所有功能

## 系統架構

### Production 環境
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PostgreSQL    │    │    Wiremock     │    │   主應用程式    │
│   (外部容器)     │    │   (API 模擬)     │    │   (prod profile) │
│   Port: 5432    │    │   Port: 8081    │    │   Port: 8888    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Dev 環境
```
┌─────────────────────────────────────────────────────────────┐
│                    主應用程式容器                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐  │
│  │  Testcontainers │  │   Mock API      │  │  主程式      │  │
│  │  (PostgreSQL)   │  │   (內部服務)     │  │ (dev profile) │  │
│  └─────────────────┘  └─────────────────┘  └──────────────┘  │
│  Port: 8889                                         │
└─────────────────────────────────────────────────────────────┘
```

## 資料庫結構

### 主要表格
- **users**: 用戶資料 (Member/Librarian)
- **books**: 書籍資料
- **libraries**: 圖書館資料
- **book_library**: 書籍與圖書館關聯
- **borrows**: 借閱記錄

### 借閱規則
- **圖書**: 每人最多 5 本，借期 1 個月
- **書籍**: 每人最多 10 本，借期 1 個月
- **逾期通知**: 到期前 5 天自動提醒

## 故障排除

### 常見問題

#### 1. Port被佔用
```bash
# 檢查Port是否佔用
lsof -i :8888
lsof -i :8889

# 停止佔用Port的Process
pkill -f "online-city-library-lending-system"
```

#### 2. Docker 容器無法啟動
```bash
# 檢查 Docker 狀態
docker ps -a

# 重新Build容器
docker-compose -f docker-compose-prd.yml up --build -d
```

#### 3. 資料庫連接失敗 or 其他相關錯誤
```bash
# 檢查 PostgreSQL Log
docker-compose -f docker-compose-prd.yml logs postgres

# 重建資料庫 (Schema / data)
docker-compose -f docker-compose-prd.yml down -v
docker-compose -f docker-compose-prd.yml up -d --build
```

## 開發說明

### DB Schema ERD
![alt text](https://github.com/cancerpio/line-assignment/blob/docker_switch_dev_prod/ERD.png?raw=true)

#### 資料表說明
- **users**: 使用者基本資料，包含角色(Librarian/Member)、帳號密碼等資訊
- **book_categories**: 書籍類型定義
- **books**: 書目基本資料，包含書名、作者、出版年份等資訊
- **branches**: 圖書館分館資訊，記錄各分館名稱
- **book_copies**: 書籍實體副本，記錄每本書在各分館的實際館藏狀態
- **loans**: 借閱紀錄，追蹤每筆借閱資訊(借出時間、到期時間、歸還時間)
### 使用技術
- **API Server**: Java 21 + Spring Boot 3.3.2
- **Database**: PostgreSQL 16
- **Security**: Spring Security + JWT
- **Container**: Docker + Docker Compose
- **Testing**: Testcontainers

### 專案結構
```
src/
├── main/
│   ├── java/com/library/
│   │   ├── auth/          # 認證相關
│   │   ├── book/          # 書籍管理
│   │   ├── borrow/        # 借閱管理
│   │   ├── library/       # 分館管理
│   │   └── user/          # 用戶管理
│   └── resources/
│       ├── application.properties
│       ├── application-dev.properties
│       ├── application-prod.properties
│       ├── schema.sql
│       └── data.sql
└── test/
    └── java/com/library/
        └── auth/
            └── JwtTest.java
```

### 配置說明
- **application.properties**: 通用配置
- **application-dev.properties**: 開發環境 (Testcontainers + Mock API)
- **application-prod.properties**: 正式環境 (外部 PostgreSQL + Wiremock)

### 悲觀鎖控制與交易管理

#### 借閱與還書的 Transaction 和 Row Lock 機制

系統在借閱和還書操作中進行併發控制，確保資料一致性：

**1. Transaction 管理**
- 使用 `@Transactional` 確保借閱和還書操作的原子性
- 借閱時：同時更新 `loans` 表和 `book_copies` Table的狀態
- 還書時：同時更新 `loans` 表的歸還時間和 `book_copies` Table的狀態
- 確保「全有全無」，只會全部更新成功或全部Rollback

**2. Row Lock 機制**
- 使用 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 實現悲觀鎖定
- 借閱時：鎖定 `book_copies` Table的Row，防止同一副本被多個使用者同時借閱
- 還書時：鎖定 `loans` Table的相關Row，防止同一借閱記錄被多個交易同時修改

**3. 實現細節**
```java
// 借閱時的 Row Lock
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT bc FROM InventoryItem bc WHERE bc.id IN :copyIds ORDER BY bc.id")
List<InventoryItem> findAndLockCopies(@Param("copyIds") List<Long> copyIds);

// 還書時的 Row Lock  
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT l.id as loanId, l.inventoryItem.id as copyId, l.inventoryItem.status as copyStatus " +
       "FROM Loan l JOIN l.inventoryItem bc " +
       "WHERE l.user.id = :userId AND l.returnedDate IS NULL " +
       "AND l.inventoryItem.id IN :copyIds ORDER BY l.inventoryItem.id")
List<Object[]> findAndLockActiveLoans(@Param("userId") Long userId, @Param("copyIds") List<Long> copyIds);
```

---
