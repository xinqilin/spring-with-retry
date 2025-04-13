package com.bill.springretry.custom;

import com.bill.springretry.exception.RemoteServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class HttpStatusCodeRetryPolicyTest {

    @Test
    void testCanRetry_RetryableStatusCode() {
        // 創建可重試的HTTP狀態碼重試策略 (503, 504是可重試的)
        int[] retryableStatusCodes = {503, 504};
        HttpStatusCodeRetryPolicy policy = new HttpStatusCodeRetryPolicy(3, retryableStatusCodes);
        
        // 創建重試上下文
        RetryContext context = policy.open(null);
        
        // 註冊一個可重試的狀態碼異常
        RemoteServiceException exception = new RemoteServiceException("服務不可用", 503);
        policy.registerThrowable(context, exception);
        
        // 驗證可以重試
        assertTrue(policy.canRetry(context), "應該可以重試503狀態碼");
    }

    @Test
    void testCanRetry_NonRetryableStatusCode() {
        // 創建可重試的HTTP狀態碼重試策略 (只有503, 504是可重試的)
        int[] retryableStatusCodes = {503, 504};
        HttpStatusCodeRetryPolicy policy = new HttpStatusCodeRetryPolicy(3, retryableStatusCodes);
        
        // 創建重試上下文
        RetryContext context = policy.open(null);
        
        // 註冊一個不可重試的狀態碼異常 (400)
        RemoteServiceException exception = new RemoteServiceException("無效請求", 400);
        policy.registerThrowable(context, exception);
        
        // 驗證不應該重試
        assertFalse(policy.canRetry(context), "不應該重試400狀態碼");
    }

    @Test
    void testCanRetry_MaxAttemptsExceeded() {
        // 創建最大嘗試次數為2的重試策略
        int[] retryableStatusCodes = {503};
        HttpStatusCodeRetryPolicy policy = new HttpStatusCodeRetryPolicy(2, retryableStatusCodes);
        
        // 創建重試上下文
        RetryContext context = policy.open(null);
        
        // 註冊兩次可重試的異常，超過了最大嘗試次數
        RemoteServiceException exception = new RemoteServiceException("服務不可用", 503);
        policy.registerThrowable(context, exception);
        policy.registerThrowable(context, exception);
        
        // 驗證不應該再重試
        assertFalse(policy.canRetry(context), "超過最大嘗試次數後不應該重試");
    }

    @Test
    void testCanRetry_NonRemoteServiceException() {
        // 創建HTTP狀態碼重試策略
        int[] retryableStatusCodes = {503};
        HttpStatusCodeRetryPolicy policy = new HttpStatusCodeRetryPolicy(3, retryableStatusCodes);
        
        // 創建重試上下文
        RetryContext context = policy.open(null);
        
        // 註冊一個非RemoteServiceException類型的異常
        RuntimeException exception = new RuntimeException("一般異常");
        policy.registerThrowable(context, exception);
        
        // 驗證不應該重試
        assertFalse(policy.canRetry(context), "非RemoteServiceException不應該重試");
    }
}
