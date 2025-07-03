package com.example.bank_batch.batch;

import com.example.bank_batch.model.Transaction;
import com.example.bank_batch.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.ArrayFieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class BatchConfig {

    private final TransactionRepository transactionRepository;
    private final JobCompletionListener jobCompletionListener;

    @Autowired
    public BatchConfig(TransactionRepository transactionRepository, JobCompletionListener jobCompletionListener) {
        this.transactionRepository = transactionRepository;
        this.jobCompletionListener = jobCompletionListener;
    }

    @Bean
    public FlatFileItemReader<String[]> reader() {
        ClassPathResource resource = new ClassPathResource("dataSource.txt");
        log.info("Reading from: {}", resource.getDescription());

        return new FlatFileItemReaderBuilder<String[]>()
                .name("transactionItemReader")
                .resource(resource)
                .linesToSkip(2) // Skip header lines
                .lineTokenizer(new DelimitedLineTokenizer("|") {{
                    setNames("accountNumber", "trxAmount", "description", "trxDate", "trxTime", "customerId");
                    setStrict(false); // Allow variable columns
                }})
                .fieldSetMapper(new ArrayFieldSetMapper())
                .strict(false) // Tolerate missing files
                .encoding("UTF-8")
                .build();
    }

    @Bean
    public ItemProcessor<String[], Transaction> processor() {
        return fields -> {
            log.info("PROCESSING RECORD: {}", String.join("|", fields));

            // Skip empty records
            if (fields.length < 6) {
                log.info("SKIPPING: INSUFFICIENT FIELDS");
                return null;
            }

            String accountNumber = fields[0];
            String trxAmountStr = fields[1];
            String description = fields[2];
            String trxDateStr = fields[3];
            String trxTimeStr = fields[4];
            String customerId = fields[5];

            // Validate required fields
            if (!StringUtils.hasText(accountNumber)) {
                log.info("SKIPPING: MISSING ACCOUNT NUMBER");
                return null;
            }
            if (!StringUtils.hasText(trxAmountStr)) {
                log.info("SKIPPING: MISSING AMOUNT");
                return null;
            }
            if (!StringUtils.hasText(trxDateStr)) {
                log.info("SKIPPING: MISSING DATE");
                return null;
            }
            if (!StringUtils.hasText(trxTimeStr)) {
                log.info("SKIPPING: MISSING TIME");
                return null;
            }

            Transaction transaction = new Transaction();
            transaction.setAccountNumber(accountNumber);

            try {
                transaction.setTrxAmount(new BigDecimal(trxAmountStr));
            } catch (NumberFormatException e) {
                log.info("SKIPPING: INVALID AMOUNT FORMAT: {}", trxAmountStr);
                return null;
            }

            transaction.setDescription(description);

            // Parse date and time
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            try {
                transaction.setTrxDate(LocalDate.parse(trxDateStr, dateFormatter));
            } catch (DateTimeParseException e) {
                log.info("SKIPPING: INVALID DATE FORMAT: {}", trxDateStr);
                return null;
            }

            try {
                transaction.setTrxTime(LocalTime.parse(trxTimeStr, timeFormatter));
            } catch (DateTimeParseException e) {
                log.info("SKIPPING: INVALID TIME FORMAT: {}", trxTimeStr);
                return null;
            }

            transaction.setCustomerId(customerId);
            return transaction;
        };
    }

    @Bean
    public ItemWriter<Transaction> writer() {
        return chunk -> {
            log.info("WRITING CHUNK OF {} ITEMS", chunk.size());

            List<Transaction> validTransactions = new ArrayList<>();
            for (Transaction transaction : chunk) {
                if (transaction != null) {
                    log.info("SAVING: {}", transaction.getAccountNumber());
                    validTransactions.add(transaction);
                }
            }

            if (!validTransactions.isEmpty()) {
                transactionRepository.saveAll(validTransactions);
                log.info("SAVED {} TRANSACTIONS", validTransactions.size());
            }
        };
    }

    @Bean
    public Job importTransactionJob(JobRepository jobRepository,
                                    Step step1) {
        return new JobBuilder("importTransactionJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step1)
                .listener(jobCompletionListener)
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      ItemReader<String[]> reader,
                      ItemProcessor<String[], Transaction> processor,
                      ItemWriter<Transaction> writer) {
        return new StepBuilder("step1", jobRepository)
                .<String[], Transaction>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(Throwable.class)
                .skipLimit(1000)
                .build();
    }
}