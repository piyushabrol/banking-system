package com.banking.banking_system.controller;

import com.banking.banking_system.entity.Account;
import com.banking.banking_system.service.AccountService;
import com.banking.banking_system.dto.WithdrawalRequest;
import org.springframework.web.bind.annotation.*;

import com.banking.banking_system.dto.DepositRequest;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @PostMapping
    public Account createAccount(@RequestBody Account account) {
        return accountService.createAccount(account);
    }

    @PostMapping("/{accountNumber}/deposit")
    public Account depositMoney(
            @PathVariable String accountNumber,
            @RequestBody DepositRequest request) {

        return accountService.depositMoney(accountNumber, request.getAmount());
    }

    @PostMapping("/{accountNumber}/withdraw")
    public Account withdrawMoney(
            @PathVariable String accountNumber,
            @RequestBody WithdrawalRequest request) {

        return accountService.withdrawMoney(
                accountNumber,
                request.getAmount());
    }
}
