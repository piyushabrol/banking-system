package com.banking.banking_system.dto;

import java.math.BigDecimal;

public class WithdrawalRequest {

    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
