package com.bill.springretry.custom;

import com.bill.springretry.exception.RemoteServiceException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Bill.Lin 2025/4/13
 * 使用自定義重試策略與退避策略的服務
 */
@Service
public class CustomRetryService {

    private final RetryTemplate httpRetryTemplate;
    private final RetryTemplate timeAwareRetryTemplate;
    
    public CustomRetryService() {
        // 創建使用 HTTP 狀態碼重試策略的模板
        this.httpRetryTemplate = new RetryTemplate();
        
        // 設置自定義的 HTTP 狀態碼重試策略
        // 只有當狀態碼為 429 (Too Many Requests), 503 (Service Unavailable), 504 (Gateway Timeout) 時才重試
        int[] retryableStatusCodes = {429, 503, 504};
        HttpStatusCodeRetryPolicy httpRetryPolicy = new HttpStatusCodeRetryPolicy(5, retryableStatusCodes);
        this.httpRetryTemplate.setRetryPolicy(httpRetryPolicy);
        
        // 使用標準的指數退避策略
        org.springframework.retry.backoff.ExponentialBackOffPolicy backOffPolicy = 
                new org.springframework.retry.backoff.ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(100); // 100ms 初始間隔
        backOffPolicy.setMultiplier(2.0);      // 每次重試間隔加倍
        backOffPolicy.setMaxInterval(10_000);  // 最大 10 秒間隔
        this.httpRetryTemplate.setBackOffPolicy(backOffPolicy);
        
        // 創建使用時間感知退避策略的模板
        this.timeAwareRetryTemplate = new RetryTemplate();
        
        // 使用標準的簡單重試策略
        org.springframework.retry.policy.SimpleRetryPolicy simpleRetryPolicy = 
                new org.springframework.retry.policy.SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(4);
        this.timeAwareRetryTemplate.setRetryPolicy(simpleRetryPolicy);
        
        // 設置自定義的時間感知退避策略
        TimeAwareBackOffPolicy timeAwareBackOffPolicy = new TimeAwareBackOffPolicy(
                1000,  // 基本 1 秒退避
                500,   // 夜間 0.5 秒退避
                9,     // 高峰時段從 9 點開始
                18,    // 高峰時段到 18 點結束
                3.0f   // 高峰時段退避時間是基本時間的 3 倍
        );
        this.timeAwareRetryTemplate.setBackOffPolicy(timeAwareBackOffPolicy);
    }
    
    /**
     * 模擬 HTTP 請求，使用自定義的 HTTP 重試策略
     */
    public <T> T executeHttpRequest(RetryCallback<T, Exception> callback) throws Exception {
        return httpRetryTemplate.execute(callback);
    }
    
    /**
     * 模擬耗時操作，使用時間感知退避策略
     */
    public <T> T executeTimeAwareOperation(RetryCallback<T, Exception> callback) throws Exception {
        return timeAwareRetryTemplate.execute(callback);
    }
    
    /**
     * 模擬 HTTP 請求
     * @param statusCode 返回的 HTTP 狀態碼
     * @param successAfterRetries 在多少次重試後成功 (-1 表示始終失敗)
     */
    public void simulateHttpRequest(int statusCode, int successAfterRetries) throws Exception {
        final int[] attemptCount = {0}; // 使用數組來存儲嘗試次數
        
        try {
            String result = executeHttpRequest(new RetryCallback<String, Exception>() {
                @Override
                public String doWithRetry(RetryContext context) throws Exception {
                    attemptCount[0]++;
                    System.out.println("發送 HTTP 請求, 嘗試 #" + attemptCount[0]);
                    
                    // 如果達到成功條件，返回成功
                    if (successAfterRetries >= 0 && attemptCount[0] > successAfterRetries) {
                        System.out.println("HTTP 請求成功!");
                        return "HTTP 請求成功";
                    }
                    
                    // 否則拋出異常
                    throw new RemoteServiceException("服務暫時不可用", statusCode);
                }
            });
            
            System.out.println("最終結果: " + result);
        } catch (Exception e) {
            System.err.println("HTTP 請求最終失敗: " + e.getMessage());
            throw e;
        }
    }
}
