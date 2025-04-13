package com.bill.springretry.custom;

import com.bill.springretry.exception.RemoteServiceException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.context.RetryContextSupport;

/**
 * @author Bill.Lin 2025/4/13
 * 自定義重試策略 - 根據 HTTP 狀態碼決定是否重試
 */
public class HttpStatusCodeRetryPolicy implements RetryPolicy {

    private final int maxAttempts;
    
    // 可重試的 HTTP 狀態碼 (通常是那些暫時性錯誤)
    private final int[] retryableStatusCodes;

    /**
     * 建立 HTTP 狀態碼重試策略
     * 
     * @param maxAttempts 最大嘗試次數
     * @param retryableStatusCodes 可重試的 HTTP 狀態碼列表
     */
    public HttpStatusCodeRetryPolicy(int maxAttempts, int[] retryableStatusCodes) {
        this.maxAttempts = maxAttempts;
        this.retryableStatusCodes = retryableStatusCodes;
    }

    @Override
    public boolean canRetry(RetryContext context) {
        // 檢查重試次數是否超過上限
        HttpRetryContext httpContext = (HttpRetryContext) context;
        if (httpContext.getRetryCount() >= maxAttempts) {
            return false;
        }
        
        // 如果沒有異常，不需要重試
        if (httpContext.getLastThrowable() == null) {
            return true;
        }
        
        // 檢查是否是 RemoteServiceException 以及狀態碼是否可重試
        if (httpContext.getLastThrowable() instanceof RemoteServiceException) {
            RemoteServiceException ex = (RemoteServiceException) httpContext.getLastThrowable();
            int statusCode = ex.getStatusCode();
            
            for (int retryableStatusCode : retryableStatusCodes) {
                if (statusCode == retryableStatusCode) {
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public RetryContext open(RetryContext parent) {
        return new HttpRetryContext(parent);
    }

    @Override
    public void close(RetryContext context) {
        // 在重試結束時執行的操作
        HttpRetryContext httpContext = (HttpRetryContext) context;
        System.out.println("HTTP 重試結束，嘗試次數: " + httpContext.getRetryCount());
    }

    @Override
    public void registerThrowable(RetryContext context, Throwable throwable) {
        HttpRetryContext httpContext = (HttpRetryContext) context;
        httpContext.registerThrowable(throwable);
        
        // 記錄 HTTP 狀態碼
        if (throwable instanceof RemoteServiceException) {
            RemoteServiceException ex = (RemoteServiceException) throwable;
            httpContext.setLastStatusCode(ex.getStatusCode());
            System.out.println("HTTP 請求失敗，狀態碼: " + ex.getStatusCode() + 
                    ", 嘗試次數: " + httpContext.getRetryCount());
        }
    }
    
    /**
     * 擴展 RetryContext 以支持 HTTP 狀態碼
     */
    private static class HttpRetryContext extends RetryContextSupport {
        
        private int lastStatusCode;
        
        public HttpRetryContext(RetryContext parent) {
            super(parent);
            this.lastStatusCode = -1;
        }
        
        public int getLastStatusCode() {
            return lastStatusCode;
        }
        
        public void setLastStatusCode(int lastStatusCode) {
            this.lastStatusCode = lastStatusCode;
        }
    }
}
