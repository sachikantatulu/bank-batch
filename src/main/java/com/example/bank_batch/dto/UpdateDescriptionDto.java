package com.example.bank_batch.dto;

import lombok.Data;

@Data
public class UpdateDescriptionDto {
    private String newDescription;
    private Integer version; // For Optimistic Locking
}