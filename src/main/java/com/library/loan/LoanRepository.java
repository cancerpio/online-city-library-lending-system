package com.library.loan;

import com.library.inventory.InventoryItem;
import com.library.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserAndReturnedDateIsNull(User user);

    List<Loan> findByDueDate(LocalDate dueDate);

    /**
     * 前置檢查：驗證副本存在和分類計數
     */
    @Query("SELECT COUNT(bc) FROM InventoryItem bc WHERE bc.id IN :copyIds")
    long countExistingCopies(@Param("copyIds") List<Long> copyIds);

    /**
     * 借書Transaction: 鎖定副本並檢查是否可借
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bc FROM InventoryItem bc WHERE bc.id IN :copyIds ORDER BY bc.id")
    List<InventoryItem> findAndLockCopies(@Param("copyIds") List<Long> copyIds);

    /**
     * 查詢使用者對應副本的未歸還借閱記錄
     */
    @Query("SELECT l FROM Loan l WHERE l.user.id = :userId AND l.inventoryItem.id = :copyId AND l.returnedDate IS NULL")
    Optional<Loan> findByUserIdAndCopyIdAndReturnedDateIsNull(@Param("userId") Long userId, @Param("copyId") Long copyId);

    /**
     * 還書Transaction：Row Lock鎖定在借中的對應副本
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l.id as loanId, l.inventoryItem.id as copyId, l.inventoryItem.status as copyStatus " +
           "FROM Loan l " +
           "JOIN l.inventoryItem bc " +
           "WHERE l.user.id = :userId " +
           "AND l.returnedDate IS NULL " +
           "AND l.inventoryItem.id IN :copyIds " +
           "ORDER BY l.inventoryItem.id")
    List<Object[]> findAndLockActiveLoans(@Param("userId") Long userId, @Param("copyIds") List<Long> copyIds);

    /**
     * 更新借閱記錄的歸還時間
     */
    @Modifying
    @Query("UPDATE Loan l SET l.returnedDate = CURRENT_DATE " +
           "WHERE l.user.id = :userId " +
           "AND l.returnedDate IS NULL " +
           "AND l.inventoryItem.id IN :copyIds")
    int updateReturnedDate(@Param("userId") Long userId, @Param("copyIds") List<Long> copyIds);

    /**
     * 更新副本狀態為可用
     */
    @Modifying
    @Query("UPDATE InventoryItem bc SET bc.status = 'AVAILABLE' " +
           "WHERE bc.id IN :copyIds")
    int updateCopyStatusToAvailable(@Param("copyIds") List<Long> copyIds);

    /**
     * 查詢所有未歸還且已逾期的借閱記錄
     */
    @Query("SELECT l FROM Loan l WHERE l.returnedDate IS NULL AND l.dueDate < :currentDate")
    List<Loan> findOverdueLoans(@Param("currentDate") LocalDate currentDate);

    /**
     * 查詢所有未歸還且已逾期的借閱記錄（使用當前日期）
     */
    default List<Loan> findOverdueLoans() {
        return findOverdueLoans(LocalDate.now());
    }

    /**
     * 查詢所有未歸還且到期日在未來5天內或已逾期的借閱記錄
     */
    @Query("SELECT l FROM Loan l WHERE l.returnedDate IS NULL AND l.dueDate <= :fiveDaysFromNow")
    List<Loan> findLoansToNotify(@Param("fiveDaysFromNow") LocalDate fiveDaysFromNow);

    /**
     * 查詢所有未歸還且到期日在未來5天內或已逾期的借閱記錄（使用當前日期）
     */
    default List<Loan> findLoansToNotify() {
        return findLoansToNotify(LocalDate.now().plusDays(5));
    }

    /**
     * 查詢使用者當前未歸還的借閱記錄
     */
    @Query("SELECT l FROM Loan l WHERE l.user.id = :userId AND l.returnedDate IS NULL")
    List<Loan> findByUserIdAndReturnedDateIsNull(@Param("userId") Long userId);
}


