
package com.banking.banking_system.service;

import com.banking.banking_system.entity.Account;
import com.banking.banking_system.entity.Transaction;
import com.banking.banking_system.repository.AccountRepository;
import com.banking.banking_system.repository.TransactionRepository;
import com.banking.banking_system.exception.InsufficientBalanceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository) {

        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional
    public Account depositMoney(String accountNumber, BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "Deposit amount must be greater than zero");
        }

        Account account = accountRepository
                .findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new RuntimeException(
                    "Cannot deposit into an inactive account");
        }

        account.setBalance(account.getBalance().add(amount));

        accountRepository.save(account);

        Transaction transaction = new Transaction();

        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setDestinationAccount(account);
        transaction.setAmount(amount);
        transaction.setType("DEPOSIT");
        transaction.setStatus("SUCCESS");

        transactionRepository.save(transaction);

        return account;
    }

    @Transactional
    public Account withdrawMoney(String accountNumber, BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "Withdrawal amount must be greater than zero");
        }

        Account account = accountRepository
                .findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new RuntimeException(
                    "Cannot withdraw from an inactive account");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));

        accountRepository.save(account);

        Transaction transaction = new Transaction();

        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setSourceAccount(account);
        transaction.setAmount(amount);
        transaction.setType("WITHDRAWAL");
        transaction.setStatus("SUCCESS");

        transactionRepository.save(transaction);

        return account;
    }

}
