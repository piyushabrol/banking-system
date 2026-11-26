
package com.banking.banking_system.controller;

import com.banking.banking_system.entity.Transaction;
import com.banking.banking_system.service.TransactionService;
import com.banking.banking_system.dto.TransferRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return transactionService.transferMoney(request);
    }
}
