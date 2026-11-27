package com.banking.banking_system.exception;

public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String message) {
        super(message);
    }
}