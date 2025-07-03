package com.example.bank_batch.service;

import com.example.bank_batch.exception.ConcurrentUpdateException;
import com.example.bank_batch.model.Transaction;
import com.example.bank_batch.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public Page<Transaction> searchTransactions(
            String customerId, String accountNumber, String description, Pageable pageable) {
        return transactionRepository.searchTransactions(customerId, accountNumber, description, pageable);
    }

    @Transactional
    public Transaction updateDescription(Long id, String newDescription, Integer version) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        if(!transaction.getVersion().equals(version)) {
            throw new ConcurrentUpdateException("Transaction was updated by another user");
        }

        transaction.setDescription(newDescription);
        return transactionRepository.save(transaction);
    }
}