
package com.banking.banking_system.controller;

import com.banking.banking_system.entity.Transaction;
import com.banking.banking_system.service.TransactionService;
import com.banking.banking_system.dto.TransferRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<Transaction> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    @PostMapping("/transfer")
    public Transaction transferMoney(@RequestBody TransferRequest request) {

        try {
            return transactionService.transferMoney(request);

        } catch (DataIntegrityViolationException e) {

            Optional<Transaction> existingTransaction =
                    transactionService.getTransactionByTransactionId(
                            request.getTransactionId()
                    );

            if (existingTransaction.isPresent()) {
                return existingTransaction.get();
            }

            throw e;
        }
    }

    @GetMapping("/account/{accountNumber}")
    public List<Transaction> getTransactionsByAccountNumber(
            @PathVariable String accountNumber) {

        return transactionService.getTransactionsByAccountNumber(accountNumber);
    }
}
