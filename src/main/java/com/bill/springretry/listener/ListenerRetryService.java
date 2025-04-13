package com.bill.springretry.listener;

import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * @author Bill.Lin 2025/4/13
 * 使用監聽器的重試服務
 */
@Service
public class ListenerRetryService {

    private final RetryTemplate retryTemplate;
    private final RetryMetricsListener metricsListener;
    private final Random random = new Random();
    
    public ListenerRetryService() {
        this.retryTemplate = new RetryTemplate();
        
        // 設置基本的重試策略
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(5);
        this.retryTemplate.setRetryPolicy(retryPolicy);
        
        // 設置固定的退避策略
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(100); // 100ms 的固定間隔
        this.retryTemplate.setBackOffPolicy(backOffPolicy);
        
        // 添加監聽器
        this.retryTemplate.registerListener(new RetryLoggingListener());
        this.metricsListener = new RetryMetricsListener();
        this.retryTemplate.registerListener(this.metricsListener);
    }
    
    /**
     * 執行具有監聽器的重試操作
     */
    public <T> T executeWithListeners(String operationName, RetryCallback<T, Exception> callback) throws Exception {
        return retryTemplate.execute(context -> {
            // 設置操作名稱，讓監聽器能夠識別
            context.setAttribute("operationName", operationName);
            return callback.doWithRetry(context);
        });
    }
    
    /**
     * 執行模擬操作
     */
    public void simulateOperations() throws Exception {
        // 模擬多種操作
        String[] operations = {"database-query", "api-call", "file-read", "processing"};
        
        for (int i = 0; i < 10; i++) {
            String operation = operations[random.nextInt(operations.length)];
            boolean willSucceed = random.nextDouble() > 0.3; // 70% 成功率
            int maxFailures = random.nextInt(4); // 0-3 次失敗
            
            try {
                executeWithListeners(operation, new RetryCallback<String, Exception>() {
                    private int attempts = 0;
                    
                    @Override
                    public String doWithRetry(RetryContext context) throws Exception {
                        attempts++;
                        
                        // 模擬可能的失敗
                        if (!willSucceed || attempts <= maxFailures) {
                            throw new RuntimeException(operation + " 操作失敗，嘗試 #" + attempts);
                        }
                        
                        return operation + " 操作成功";
                    }
                });
            } catch (Exception e) {
                // 預期的異常，不需處理
            }
        }
        
        // 打印統計信息
        metricsListener.printStats();
    }
}
