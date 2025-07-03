package com.example.bank_batch.batch;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JobCompletionListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        JobParameters params = jobExecution.getJobParameters();
        String inputFile = params.getString("dataSource.txt");
        log.info("Processing file: {}", inputFile);
        log.info("Batch job starting: {}", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == org.springframework.batch.core.BatchStatus.COMPLETED) {
            jobExecution.getStepExecutions().forEach(stepExecution -> {
                log.info("Step {} processed {} items",
                        stepExecution.getStepName(),
                        stepExecution.getWriteCount());
            });
            log.info("BATCH JOB COMPLETED SUCCESSFULLY");
        } else if (jobExecution.getStatus() == org.springframework.batch.core.BatchStatus.FAILED) {
            jobExecution.getAllFailureExceptions().forEach(ex ->
                    log.error("BATCH JOB FAILED: ", ex)
            );
        }
    }
}
