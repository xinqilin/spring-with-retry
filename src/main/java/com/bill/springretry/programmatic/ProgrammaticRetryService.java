package com.bill.springretry.programmatic;

import com.bill.springretry.exception.DatabaseException;
import org.springframework.classify.Classifier;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bill.Lin 2025/4/13
 * 示範如何以程式碼方式使用 RetryTemplate 進行重試操作
 */
@Service
public class ProgrammaticRetryService {

    private final RetryTemplate simpleRetryTemplate;
    private final RetryTemplate customRetryTemplate;

    public ProgrammaticRetryService() {
        // 設置簡單的重試模板
        this.simpleRetryTemplate = new RetryTemplate();
        
        // 配置重試策略 - 最多重試3次
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        
        // 配置退避策略 - 指數退避
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000); // 初始間隔1秒
        backOffPolicy.setMultiplier(2);         // 每次間隔加倍
        backOffPolicy.setMaxInterval(10000);    // 最大間隔10秒
        
        this.simpleRetryTemplate.setRetryPolicy(retryPolicy);
        this.simpleRetryTemplate.setBackOffPolicy(backOffPolicy);
        
        // 設置自訂重試模板 - 根據異常類型決定不同的重試策略
        this.customRetryTemplate = new RetryTemplate();
        
        // 創建分類器 - 根據異常類型選擇不同的重試策略
        ExceptionClassifierRetryPolicy classifierRetryPolicy = new ExceptionClassifierRetryPolicy();
        
        // 為不同的異常配置不同的重試策略
        Classifier<Throwable, RetryPolicy> classifier = throwable -> {
            if (throwable instanceof DatabaseException) {
                // 數據庫異常最多重試5次
                SimpleRetryPolicy dbPolicy = new SimpleRetryPolicy();
                dbPolicy.setMaxAttempts(5);
                return dbPolicy;
            }
            // 其他異常不重試
            return new NeverRetryPolicy();
        };
        
        classifierRetryPolicy.setExceptionClassifier(classifier);
        
        // 應用自訂策略
        this.customRetryTemplate.setRetryPolicy(classifierRetryPolicy);
        
        // 使用相同的退避策略
        this.customRetryTemplate.setBackOffPolicy(backOffPolicy);
    }

    /**
     * 使用簡單重試模板進行操作
     */
    public <T> T executeWithRetry(RetryCallback<T, Exception> retryCallback) throws Exception {
        return simpleRetryTemplate.execute(retryCallback);
    }

    /**
     * 使用自訂重試模板進行操作
     */
    public <T> T executeWithCustomRetry(RetryCallback<T, Exception> retryCallback) throws Exception {
        return customRetryTemplate.execute(retryCallback);
    }
    
    /**
     * 使用基於異常的重試策略 - 這種方式可以直接在方法內建立臨時的重試模板
     */
    public <T> T executeWithExceptionBasedRetry(RetryCallback<T, Exception> retryCallback) throws Exception {
        // 創建重試模板
        RetryTemplate template = new RetryTemplate();
        
        // 配置基於特定異常的重試策略
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(DatabaseException.class, true);        // 數據庫異常時重試
        retryableExceptions.put(IllegalStateException.class, false);   // 狀態異常時不重試
        
        SimpleRetryPolicy policy = new SimpleRetryPolicy(3, retryableExceptions, true);
        template.setRetryPolicy(policy);
        
        // 使用固定間隔的退避策略
        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(500);
        template.setBackOffPolicy(backOff);
        
        // 執行重試操作
        return template.execute(retryCallback, context -> {
            // 恢復回調 - 當所有重試都失敗時執行
            System.err.println("所有重試都失敗了，最後一個異常: " + context.getLastThrowable().getMessage());
            throw new RuntimeException("重試失敗", context.getLastThrowable());
        });
    }
    
    /**
     * 示範如何使用 RetryContext 在重試過程中傳遞狀態
     */
    public <T> T executeWithContext(RetryCallback<T, Exception> retryCallback) throws Exception {
        return simpleRetryTemplate.execute(context -> {
            // 在重試上下文中設置屬性
            context.setAttribute("startTime", System.currentTimeMillis());
            
            // 執行可能失敗的操作
            try {
                return retryCallback.doWithRetry(context);
            } catch (Exception e) {
                // 記錄失敗信息
                System.err.println("嘗試失敗，這是第 " + context.getRetryCount() + " 次重試");
                System.err.println("從開始到現在耗時: " + 
                        (System.currentTimeMillis() - (long) context.getAttribute("startTime")) + "ms");
                
                // 重新拋出異常以觸發重試
                throw e;
            }
        });
    }
}
