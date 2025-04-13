package com.bill.springretry.configuration;

import com.bill.springretry.exception.DatabaseException;
import com.bill.springretry.exception.RemoteServiceException;
import com.bill.springretry.exception.TransientNetworkException;
import com.bill.springretry.listener.RetryLoggingListener;
import com.bill.springretry.listener.RetryMetricsListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bill.Lin 2025/4/13
 * 基於配置的重試機制
 * 使用 Spring 的依賴注入配置不同類型的重試模板
 */
@Configuration
public class RetryConfiguration {

    /**
     * 簡單的重試模板，使用固定的退避策略
     */
    @Bean(name = "simpleRetryTemplate")
    public RetryTemplate simpleRetryTemplate() {
        RetryTemplate template = new RetryTemplate();
        
        // 配置重試策略
        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(3);
        template.setRetryPolicy(policy);
        
        // 配置退避策略
        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(1000); // 1秒的固定間隔
        template.setBackOffPolicy(backOff);
        
        return template;
    }
    
    /**
     * 資料庫操作的重試模板，針對數據庫異常進行優化
     */
    @Bean(name = "databaseRetryTemplate")
    public RetryTemplate databaseRetryTemplate() {
        RetryTemplate template = new RetryTemplate();
        
        // 配置針對數據庫異常的重試策略
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(DatabaseException.class, true);
        
        SimpleRetryPolicy policy = new SimpleRetryPolicy(5, retryableExceptions);
        template.setRetryPolicy(policy);
        
        // 配置指數退避策略
        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(100);    // 初始 100ms
        backOff.setMultiplier(2.0);         // 每次加倍
        backOff.setMaxInterval(30000);      // 最大 30秒
        template.setBackOffPolicy(backOff);
        
        return template;
    }
    
    /**
     * 網絡操作的重試模板，針對網絡異常進行優化
     */
    @Bean(name = "networkRetryTemplate")
    public RetryTemplate networkRetryTemplate() {
        RetryTemplate template = new RetryTemplate();
        
        // 配置針對網絡異常的重試策略
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(TransientNetworkException.class, true);
        retryableExceptions.put(RemoteServiceException.class, true);
        
        SimpleRetryPolicy policy = new SimpleRetryPolicy(4, retryableExceptions);
        template.setRetryPolicy(policy);
        
        // 配置指數退避策略（帶隨機因子）
        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(500);     // 初始 500ms
        backOff.setMultiplier(1.5);          // 每次乘以 1.5
        backOff.setMaxInterval(10000);       // 最大 10秒
        
        // 添加隨機因子，避免多個客戶端同時重試
        org.springframework.retry.backoff.ExponentialRandomBackOffPolicy randomBackOff = 
                new org.springframework.retry.backoff.ExponentialRandomBackOffPolicy();
        randomBackOff.setInitialInterval(backOff.getInitialInterval());
        randomBackOff.setMultiplier(backOff.getMultiplier());
        randomBackOff.setMaxInterval(backOff.getMaxInterval());
        template.setBackOffPolicy(randomBackOff);
        
        return template;
    }
    
    /**
     * 具有監聽功能的重試模板
     */
    @Bean(name = "monitoredRetryTemplate")
    public RetryTemplate monitoredRetryTemplate() {
        RetryTemplate template = new RetryTemplate();
        
        // 使用標準的重試策略
        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(3);
        template.setRetryPolicy(policy);
        
        // 使用標準的退避策略
        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(500);
        template.setBackOffPolicy(backOff);
        
        // 添加監聽器
        template.registerListener(new RetryLoggingListener());
        template.registerListener(new RetryMetricsListener());
        
        return template;
    }
    
    /**
     * 為度量監聽器創建 Bean，以便在多個服務中共享
     */
    @Bean
    public RetryMetricsListener retryMetricsListener() {
        return new RetryMetricsListener();
    }
}
