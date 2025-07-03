package com.example.bank_batch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "trx_amount", precision = 19, scale = 4)
    private BigDecimal trxAmount;

    @Column(length = 255)
    private String description;

    @Column(name = "trx_date")
    private LocalDate trxDate;

    @Column(name = "trx_time")
    private LocalTime trxTime;

    @Column(name = "customer_id", length = 20)
    private String customerId;

    @Version
    private Integer version;
}