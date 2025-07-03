package com.example.bank_batch.repository;

import com.example.bank_batch.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE " +
            "(:customerId IS NULL OR t.customerId LIKE %:customerId%) AND " +
            "(:accountNumber IS NULL OR t.accountNumber LIKE %:accountNumber%) AND " +
            "(:description IS NULL OR t.description LIKE %:description%)")
    Page<Transaction> searchTransactions(
            @Param("customerId") String customerId,
            @Param("accountNumber") String accountNumber,
            @Param("description") String description,
            Pageable pageable);
}