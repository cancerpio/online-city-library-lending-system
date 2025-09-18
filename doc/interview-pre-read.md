# Interview Pre-read (Optional)

> 此文件僅作為面試前的摘要資料，方便面試官參考，**不含功能變更**。  
> Submission tag: `submission-v1`（原提交點）

## Work Experience (PPT)
- Slides: https://docs.google.com/presentation/d/1HYidbLgT3KfgpnCgQ-IrGV9p4jhIor6w/edit?usp=sharing&ouid=106736669961023999979&rtpof=true&sd=true

## Assignment: Functionality Improvements
1) **Row Lock (book_copies, loans) — 還書時，副本及借閱紀錄的Row Lock 方式改善**  
   現狀:
    - 在還書時，因為Query Method是以 `loans` 為 root，所以在 `SELECT` 階段時，Row Lock只鎖了 `loans`，沒有鎖到 `book_copies`。
    - 假設有兩個還書的Transaction，在這段空窗期可能同時讀到同一筆 `book_copies.status` ，之後在 `UPDATE` 才碰撞，造成等待或較易形成Deadlock。
   ```
   @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l.id as loanId, l.inventoryItem.id as copyId, l.inventoryItem.status as copyStatus " +
           "FROM Loan l " +
           "JOIN l.inventoryItem bc " +
           "WHERE l.user.id = :userId " +
           "AND l.returnedDate IS NULL " +
           "AND l.inventoryItem.id IN :copyIds " +
           "ORDER BY l.inventoryItem.id")
    List<Object[]> findAndLockActiveLoans(@Param("userId") Long userId, @Param("copyIds") List<Long> copyIds);
   ```
   改善方式:
    - A.不做 `JOIN` ，在JPA Entity分兩個Query Method，分別鎖兩個Table
       ```
        // 1) 鎖 book_copies
       @Lock(LockModeType.PESSIMISTIC_WRITE)
       @Query("SELECT bc FROM InventoryItem bc WHERE bc.id IN :copyIds")
 
       // 2) 鎖 loans
       @Lock(LockModeType.PESSIMISTIC_WRITE)
       @Query("SELECT l FROM Loan l WHERE l.inventoryItem.id IN :copyIds AND l.returnedDate IS NULL")
       ```
    - B.native SQL: 直接在 `JOIN` 的SQL用 `FOR UPDATE OF book_copies, loans` 鎖住兩個Table的Row lock
       ```
        SELECT l.id AS loan_id, l.copy_id
         FROM book_copies bc
         JOIN loans l ON l.copy_id = bc.id AND l.returned_at IS NULL
         WHERE bc.id = ANY(:copy_ids) 
         FOR UPDATE OF bc, l;
       ```

2) **Deadlock Prevention — 借還書時，避免多個副本Deadlock的方式**  
   現狀:
    - 在查詢完成時，才在SQL用 `ORDER BY` 把查詢結果做排序，並沒有避免Deadlock，應該要在 Service層就在Java端用統一的方式排序。
   ```
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT bc FROM InventoryItem bc WHERE bc.id IN :copyIds ORDER BY bc.id")
   List<InventoryItem> findAndLockCopies(@Param("copyIds") List<Long> copyIds);
   /**
   * 還書Transaction：Row Lock鎖定在借中的對應副本
   */
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT l.id as loanId, l.inventoryItem.id as copyId, l.inventoryItem.status as copyStatus " +
          ...
         "ORDER BY l.inventoryItem.id")
   ```
   改善方式: 在 Service層 前先對 `copyIds` 排序，所有交易以**相同順序**取Lock。
   ```
   // 先把lockedCopies做排序
   List<InventoryItem> lockedCopies = loanRepository.findAndLockCopies(copyIds
   ```

3) **JPA Performance Optimization - Lazy Fetching — 借書時，做了多餘的Join Table**  
   現狀:
    - 在借書流程，檢查使用者當前已借的書 / 圖書數量時，實際上我們只需要books的資訊，並不需要branch (分館資訊)。
    - 目前 `book_copies` 的JPA Entity因為在 `branch` 標記了 `@ManyToOne` 和 `@JoinColumn`，導致多join了不需要的branch 資訊。
   ```
   @ManyToOne(optional = false)
    @JoinColumn(name = "branch_id")
    private LibraryBranch branch;
   ```
   改善方式: 應該要在`branch`的 `@ManyToOne` 加上`fetch = FetchType.LAZY`，避免撈到不需要的資料。
   ```
   @ManyToOne(optional = false, fetch = FetchType.LAZY)
   @JoinColumn(name = "branch_id")
   private LibraryBranch branch;
   ```       

4) **Duplicate IDs 不該去重 — 借還書時，有重複的副本應該通知使用者**  
   現狀:
    - 在借還書流程中，會自動檢查`book_copies id`並且去掉重複的副本，所以有重複的副本並不會中斷流程並回應錯誤。
    - 但此方式可能會造成使用者誤判，以為該還的書都還了，實際上是`輸入錯誤而不自知`。
        - Ex: 應該要還副本`1,2,3`，結果API誤植成`1,2,2`，導致實際只還了`1, 2`沒還到`3`，但是API會回覆Success。

   改善方式: 有重複的副本應該回應錯誤，通知使用者。



5) **Error Handling - Inconsistent HTTP Status Codes - 部分Client端錯誤顯示成500 Internal Server Error**  
   現狀: 某些Client端錯誤（如: 新增書籍時出現錯誤: `IllegalArgumentException: 分館不存在`、借書沒指定book_copies時）落到 **500 Internal Server Error**，應該要用4xx來回應。
   改善方式:
    - 完善，`GlobalExceptionHandler` ，捕捉正確的Exception來對映 **400/409/422** 的錯誤格式
    - 統一回應格式，例如Follow `JSend`


## Code Quality Improvements

1) **Redundant Security Annotationss - 部分Client端錯誤顯示成500 Internal Server Error**  
   現狀:
    - `@PreAuthorize` annotations 沒有被使用，目前統一在`SecurityFilterChain`驗證權限，不透過Method-level security
      改善方式:
    -  移除沒有用到的annotation

2) **Entity Naming Consistency - 部分Table命名方式與JPA Entity不同**  
   現狀:
    - InventoryItem` entity maps to `book_copies` table
    - 專案初期的Naming沒有統一
      改善方式:
    - 讓JPA Entity命名和Table一致

3) **Transaction Boundary Clarity - Service內層Transaction定義**  
   現狀:
    - 內層方法標記了@Transactional
      改善方式:
    - 如果內層方法只是外層交易的一部分，可以不用標 @Transactional，因為外層已經控制了交易邊界，這樣可以更清楚誰在負責開啟交易，增加可讀性
    - 如果內層方法將來可能被獨立使用，或需要特殊的傳播行為（像 REQUIRES_NEW），那才適合單獨標 @Transactional。


## Future Considerations

- Centralized logging and monitoring
- API version maintaining and security enhancements
- Database connection pooling optimization
- Caching strategies for frequently accessed data (Ex: LFU Cache)
- API Document improving (ex: use Swagger UI)

---
