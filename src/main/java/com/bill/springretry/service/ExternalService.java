package com.bill.springretry.service;

import com.bill.springretry.exception.TransientNetworkException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * @author Bill.Lin 2025/4/13
 */
@Service
public class ExternalService {

    @Retryable(
            retryFor = {TransientNetworkException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void callExternalService() {
        System.out.println("嘗試呼叫外部服務...");
        if (getRandomValue() < 0.7) {
            throw new TransientNetworkException("模擬網絡故障");
        }
        System.out.println("外部服務呼叫成功！");
    }

    // 為了便於測試，將隨機邏輯提取到一個方法中
    protected double getRandomValue() {
        return Math.random();
    }

    @Recover
    public void recover(TransientNetworkException e) {
        System.err.println("所有重試失敗，執行恢復邏輯：" + e.getMessage());
    }
}
