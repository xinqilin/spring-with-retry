package com.bill.springretry.circuitbreaker;

import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.CircuitBreakerRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Bill.Lin 2025/4/13
 * 使用斷路器模式的重試服務
 * 斷路器模式是一種在分佈式系統中防止系統崩潰的模式，
 * 當系統發生大量錯誤時，自動切斷請求，避免更多請求進入已經故障的服務
 */
@Service
public class CircuitBreakerService {

    private final RetryTemplate retryTemplate;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    
    // 用於模擬服務健康狀態
    private volatile boolean serviceHealthy = true;
    
    public CircuitBreakerService() {
        this.retryTemplate = new RetryTemplate();
        
        // 創建基本的重試策略
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(3);
        
        // 包裝為斷路器策略
        CircuitBreakerRetryPolicy circuitBreakerRetryPolicy = new CircuitBreakerRetryPolicy(simpleRetryPolicy);
        
        // 設置打開斷路器的閾值（連續失敗次數）
        circuitBreakerRetryPolicy.setOpenTimeout(5000); // 斷路器打開後 5 秒內不允許新的請求
        circuitBreakerRetryPolicy.setResetTimeout(10000); // 10 秒後自動重置斷路器狀態
        
        this.retryTemplate.setRetryPolicy(circuitBreakerRetryPolicy);
        
        // 設置退避策略
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000); // 1 秒的固定退避時間
        this.retryTemplate.setBackOffPolicy(backOffPolicy);
    }
    
    /**
     * 執行受斷路器保護的操作
     */
    public <T> T executeWithCircuitBreaker(RetryCallback<T, Exception> callback) throws Exception {
        try {
            return retryTemplate.execute(context -> {
                printCircuitState(context);
                
                try {
                    T result = callback.doWithRetry(context);
                    // 操作成功，重置連續失敗計數
                    consecutiveFailures.set(0);
                    return result;
                } catch (Exception e) {
                    // 記錄連續失敗
                    int failures = consecutiveFailures.incrementAndGet();
                    System.err.println("操作失敗，當前連續失敗次數: " + failures);
                    
                    // 如果連續失敗超過閾值，可以手動打開斷路器
                    // 在這個例子中，我們依賴 CircuitBreakerRetryPolicy 的自動機制
                    
                    throw e;
                }
            });
        } catch (Exception e) {
            System.err.println("操作最終失敗: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * 模擬外部服務調用
     */
    public String callExternalService(String input) throws Exception {
        return executeWithCircuitBreaker(context -> {
            System.out.println("[" + new Date() + "] 調用外部服務，參數: " + input);
            
            // 檢查服務健康狀態
            if (!serviceHealthy) {
                throw new RuntimeException("外部服務不可用");
            }
            
            // 即使服務健康，仍有可能失敗
            if (Math.random() < 0.3) {
                throw new RuntimeException("隨機服務錯誤");
            }
            
            return "服務調用成功，結果: " + input.toUpperCase();
        });
    }
    
    /**
     * 設置服務健康狀態
     */
    public void setServiceHealthy(boolean healthy) {
        System.out.println("[" + new Date() + "] 設置服務健康狀態為: " + (healthy ? "健康" : "不健康"));
        this.serviceHealthy = healthy;
    }
    
    /**
     * 打印當前斷路器狀態
     */
    private void printCircuitState(RetryContext context) {
        Boolean open = (Boolean) context.getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_OPEN);
        if (open != null && open) {
            System.out.println("[" + new Date() + "] 斷路器狀態: 打開 (拒絕請求)");
        } else {
            System.out.println("[" + new Date() + "] 斷路器狀態: 關閉 (允許請求)");
        }
    }
    
    /**
     * 重置斷路器
     */
    public void resetCircuitBreaker() {
        System.out.println("[" + new Date() + "] 手動重置斷路器狀態");
        consecutiveFailures.set(0);
        // 注意：實際的 CircuitBreakerRetryPolicy 狀態是由 Spring Retry 內部管理的
        // 這裡我們只是重置了我們自己的計數器
    }
}
