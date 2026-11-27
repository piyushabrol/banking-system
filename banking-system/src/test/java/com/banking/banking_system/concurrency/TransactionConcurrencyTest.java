
package com.banking.banking_system.concurrency;

import com.banking.banking_system.dto.TransferRequest;
import com.banking.banking_system.entity.Account;
import com.banking.banking_system.entity.Customer;
import com.banking.banking_system.repository.AccountRepository;
import com.banking.banking_system.repository.CustomerRepository;
import com.banking.banking_system.repository.TransactionRepository;
import com.banking.banking_system.service.TransactionService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TransactionConcurrencyTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void testConcurrentTransfers() throws Exception {

        Customer customer = new Customer();
        customer.setName("Test Customer");
        customer.setEmail("test@example.com");
        customer = customerRepository.save(customer);

        Account account1 = new Account();
        account1.setAccountNumber("TEST1001");
        account1.setBalance(new BigDecimal("10000"));
        account1.setCustomer(customer);
        accountRepository.save(account1);

        Account account2 = new Account();
        account2.setAccountNumber("TEST1002");
        account2.setBalance(new BigDecimal("5000"));
        account2.setCustomer(customer);
        accountRepository.save(account2);

        TransferRequest request = new TransferRequest();
        request.setTransactionId("CONCURRENT_TXN_001");
        request.setFromAccountNumber("TEST1001");
        request.setToAccountNumber("TEST1002");
        request.setAmount(new BigDecimal("1000"));

        int numberOfThreads = 10;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        CountDownLatch ready = new CountDownLatch(numberOfThreads);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < numberOfThreads; i++) {

                Future<?> future = executor.submit(() -> {
                    ready.countDown();

                    try {
                        start.await();
                        transactionService.transferMoney(request);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }

                    return null;
                });

                futures.add(future);
            }

            ready.await();
            start.countDown();

            for (Future<?> future : futures) {
                future.get();
            }

        } finally {
            executor.shutdown();
        }

        Account updatedAccount1 = accountRepository
                .findByAccountNumber("TEST1001")
                .orElseThrow();

        Account updatedAccount2 = accountRepository
                .findByAccountNumber("TEST1002")
                .orElseThrow();

        assertEquals(0, updatedAccount1.getBalance()
                .compareTo(new BigDecimal("9000")));

        assertEquals(0, updatedAccount2.getBalance()
                .compareTo(new BigDecimal("6000")));

        assertTrue(transactionRepository
                .findByTransactionId("CONCURRENT_TXN_001")
                .isPresent());
    }
}
