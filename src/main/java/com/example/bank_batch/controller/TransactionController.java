package com.example.bank_batch.controller;

import com.example.bank_batch.dto.UpdateDescriptionDto;
import com.example.bank_batch.model.Transaction;
import com.example.bank_batch.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;
    private final JobLauncher jobLauncher;
    private final Job importTransactionJob;

    @PostMapping("/batch/start")
    public ResponseEntity<String> startBatchJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("startAt", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(importTransactionJob, jobParameters);
        return ResponseEntity.ok("Batch job started successfully...");
    }

    @GetMapping
    public ResponseEntity<Page<Transaction>> searchTransactions(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String description,
            Pageable pageable) {
        return ResponseEntity.ok(
                transactionService.searchTransactions(customerId, accountNumber, description, pageable)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> updateDescription(
            @PathVariable Long id,
            @RequestBody UpdateDescriptionDto updateDto) {
        return ResponseEntity.ok(
                transactionService.updateDescription(id, updateDto.getNewDescription(), updateDto.getVersion())
        );
    }
}