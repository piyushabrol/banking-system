package com.banking.banking_system.service;

import com.banking.banking_system.entity.Transaction;
import com.banking.banking_system.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import com.banking.banking_system.dto.TransferRequest;
import com.banking.banking_system.entity.Account;
import com.banking.banking_system.repository.AccountRepository;
import org.springframework.transaction.annotation.Transactional;

import com.banking.banking_system.exception.InsufficientBalanceException;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository) {

        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public Transaction transferMoney(TransferRequest request) {

        // 1. Get transfer details
        String fromAccountNumber = request.getFromAccountNumber();
        String toAccountNumber = request.getToAccountNumber();
        BigDecimal amount = request.getAmount();

        if (fromAccountNumber == null || toAccountNumber == null) {
            throw new IllegalArgumentException("Account numbers are required");
        }
        // 2. Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        // 3. Check sender and receiver are different
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new IllegalArgumentException("Cannot transfer money to the same account");
        }

        // 4. Find both accounts with database locking
        Account fromAccount = accountRepository
                .findByAccountNumberForUpdate(fromAccountNumber)
                .orElseThrow(() -> new RuntimeException("Sender account not found"));

        Account toAccount = accountRepository
                .findByAccountNumberForUpdate(toAccountNumber)
                .orElseThrow(() -> new RuntimeException("Receiver account not found"));

        // 5. Check account status
        if (!"ACTIVE".equals(fromAccount.getStatus())
                || !"ACTIVE".equals(toAccount.getStatus())) {
            throw new IllegalArgumentException("Both accounts must be ACTIVE");
        }

        // 6. Check sufficient balance
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        // 7. Deduct money from sender
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));

        // 8. Add money to receiver
        toAccount.setBalance(toAccount.getBalance().add(amount));

        // 9. Save updated accounts
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // 10. Create transaction record
        Transaction transaction = new Transaction();

        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setSourceAccount(fromAccount);
        transaction.setDestinationAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setType("TRANSFER");
        transaction.setStatus("SUCCESS");

        // 11. Save transaction
        return transactionRepository.save(transaction);
    }

}
