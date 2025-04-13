package com.bill.springretry.configuration;

import com.bill.springretry.exception.DatabaseException;
import com.bill.springretry.exception.TransientNetworkException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Bill.Lin 2025/4/13
 * 使用配置的重試模板
 */
@Service
public class ConfiguredRetryService {

    private final RetryTemplate simpleRetryTemplate;
    private final RetryTemplate databaseRetryTemplate;
    private final RetryTemplate networkRetryTemplate;
    private final RetryTemplate monitoredRetryTemplate;
    
    @Autowired
    public ConfiguredRetryService(
            @Qualifier("simpleRetryTemplate") RetryTemplate simpleRetryTemplate,
            @Qualifier("databaseRetryTemplate") RetryTemplate databaseRetryTemplate,
            @Qualifier("networkRetryTemplate") RetryTemplate networkRetryTemplate,
            @Qualifier("monitoredRetryTemplate") RetryTemplate monitoredRetryTemplate) {
        this.simpleRetryTemplate = simpleRetryTemplate;
        this.databaseRetryTemplate = databaseRetryTemplate;
        this.networkRetryTemplate = networkRetryTemplate;
        this.monitoredRetryTemplate = monitoredRetryTemplate;
    }
    
    /**
     * 執行簡單重試
     */
    public <T> T executeSimpleRetry(RetryCallback<T, Exception> callback) throws Exception {
        return simpleRetryTemplate.execute(callback);
    }
    
    /**
     * 執行資料庫重試
     */
    public <T> T executeDatabaseRetry(RetryCallback<T, Exception> callback) throws Exception {
        return databaseRetryTemplate.execute(callback);
    }
    
    /**
     * 執行網絡重試
     */
    public <T> T executeNetworkRetry(RetryCallback<T, Exception> callback) throws Exception {
        return networkRetryTemplate.execute(callback);
    }
    
    /**
     * 執行有監聽器的重試
     */
    public <T> T executeMonitoredRetry(RetryCallback<T, Exception> callback) throws Exception {
        return monitoredRetryTemplate.execute(callback);
    }
    
    /**
     * 模擬資料庫操作
     */
    public String performDatabaseOperation(String query) throws Exception {
        return executeDatabaseRetry(context -> {
            System.out.println("執行資料庫查詢: " + query + " (嘗試 #" + context.getRetryCount() + ")");
            
            // 模擬資料庫操作可能失敗
            if (Math.random() < 0.7) {
                throw new DatabaseException("資料庫連接異常: " + query);
            }
            
            return "資料庫查詢結果: " + query.toUpperCase();
        });
    }
    
    /**
     * 模擬網絡操作
     */
    public String performNetworkOperation(String url) throws Exception {
        return executeNetworkRetry(context -> {
            System.out.println("請求URL: " + url + " (嘗試 #" + context.getRetryCount() + ")");
            
            // 模擬網絡操作可能失敗
            if (Math.random() < 0.7) {
                throw new TransientNetworkException("網絡連接超時: " + url);
            }
            
            return "網絡請求結果: " + url + " 響應 200 OK";
        });
    }
    
    /**
     * 執行受監控的操作
     */
    public String performMonitoredOperation(String operation) throws Exception {
        return executeMonitoredRetry(context -> {
            context.setAttribute("operationName", "monitored-" + operation);
            System.out.println("執行受監控操作: " + operation + " (嘗試 #" + context.getRetryCount() + ")");
            
            // 模擬操作可能失敗
            if (Math.random() < 0.5) {
                throw new RuntimeException("操作失敗: " + operation);
            }
            
            return "操作結果: " + operation + " 成功";
        });
    }
}
