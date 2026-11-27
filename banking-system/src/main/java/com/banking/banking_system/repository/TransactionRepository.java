package com.banking.banking_system.repository;

import com.banking.banking_system.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySourceAccount_AccountNumberOrDestinationAccount_AccountNumberOrderByCreatedAtDesc(
            String sourceAccountNumber,
            String destinationAccountNumber);

    Optional<Transaction> findByTransactionId(String transactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.transactionId = :transactionId")
    Optional<Transaction> findByTransactionIdForUpdate(
            @Param("transactionId") String transactionId);
}