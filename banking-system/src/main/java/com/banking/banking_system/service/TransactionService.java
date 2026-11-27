
package com.banking.banking_system.service;

import com.banking.banking_system.dto.TransferRequest;
import com.banking.banking_system.entity.Account;
import com.banking.banking_system.entity.Transaction;
import com.banking.banking_system.exception.InsufficientBalanceException;
import com.banking.banking_system.repository.AccountRepository;
import com.banking.banking_system.repository.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

        public Optional<Transaction> getTransactionByTransactionId(String transactionId) {
                return transactionRepository.findByTransactionId(transactionId);
        }

        private final TransactionRepository transactionRepository;
        private final AccountRepository accountRepository;

        public TransactionService(
                        TransactionRepository transactionRepository,
                        AccountRepository accountRepository) {

                this.transactionRepository = transactionRepository;
                this.accountRepository = accountRepository;
        }

        // Get all transactions
        public List<Transaction> getAllTransactions() {
                return transactionRepository.findAll();
        }

        // Get transactions of a particular account
        public List<Transaction> getTransactionsByAccountNumber(String accountNumber) {

                // Check whether the account exists
                accountRepository.findByAccountNumber(accountNumber)
                                .orElseThrow(() -> new RuntimeException("Account not found"));

                // Find transactions where account is sender or receiver
                return transactionRepository
                                .findBySourceAccount_AccountNumberOrDestinationAccount_AccountNumberOrderByCreatedAtDesc(
                                                accountNumber,
                                                accountNumber);
        }

        // Transfer money between two accounts
        @Transactional(rollbackFor = Exception.class)
        public Transaction transferMoney(TransferRequest request) {

                // 1. Get transfer details
                String transactionId = request.getTransactionId();
                String fromAccountNumber = request.getFromAccountNumber();
                String toAccountNumber = request.getToAccountNumber();
                BigDecimal amount = request.getAmount();

                // 2. Validate transaction ID
                if (transactionId == null || transactionId.isBlank()) {
                        throw new IllegalArgumentException("Transaction ID is required");
                }

                // 3. Check whether transaction ID already exists
                var existingTransaction = transactionRepository
                                .findByTransactionId(transactionId);

                if (existingTransaction.isPresent()) {
                        return existingTransaction.get();
                }

                if (fromAccountNumber == null || toAccountNumber == null) {
                        throw new IllegalArgumentException("Account numbers are required");
                }

                // 4. Validate amount
                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException(
                                        "Transfer amount must be greater than zero");
                }

                // 5. Check sender and receiver are different
                if (fromAccountNumber.equals(toAccountNumber)) {
                        throw new IllegalArgumentException(
                                        "Cannot transfer money to the same account");
                }

                // 6. Find both accounts with database locking
                Account fromAccount = accountRepository
                                .findByAccountNumberForUpdate(fromAccountNumber)
                                .orElseThrow(() -> new RuntimeException(
                                                "Sender account not found"));

                Account toAccount = accountRepository
                                .findByAccountNumberForUpdate(toAccountNumber)
                                .orElseThrow(() -> new RuntimeException(
                                                "Receiver account not found"));

                // Recheck transaction ID after acquiring account locks
                var duplicateTransaction = transactionRepository
                                .findByTransactionIdForUpdate(transactionId);

                if (duplicateTransaction.isPresent()) {
                        return duplicateTransaction.get();
                }

                // 7. Check account status
                if (!"ACTIVE".equals(fromAccount.getStatus())
                                || !"ACTIVE".equals(toAccount.getStatus())) {

                        throw new IllegalArgumentException(
                                        "Both accounts must be ACTIVE");
                }

                // 8. Check sufficient balance
                if (fromAccount.getBalance().compareTo(amount) < 0) {
                        throw new InsufficientBalanceException(
                                        "Insufficient balance");
                }

                // 9. Deduct money from sender
                fromAccount.setBalance(
                                fromAccount.getBalance().subtract(amount));

                // 10. Add money to receiver
                toAccount.setBalance(
                                toAccount.getBalance().add(amount));

                // 11. Save updated accounts
                accountRepository.save(fromAccount);
                accountRepository.save(toAccount);

                // 12. Create transaction record
                Transaction transaction = new Transaction();

                transaction.setTransactionId(transactionId);
                transaction.setSourceAccount(fromAccount);
                transaction.setDestinationAccount(toAccount);
                transaction.setAmount(amount);
                transaction.setType("TRANSFER");
                transaction.setStatus("SUCCESS");

                // 13. Save transaction
                return transactionRepository.save(transaction);
        }
}
