package com.bill.springretry.configuration;

import com.bill.springretry.listener.RetryMetricsListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * @author Bill.Lin 2025/4/13
 * 演示基於配置的重試運行器
 */
@Component
@Profile("configuration")
public class ConfiguredRetryRunner implements CommandLineRunner {
    
    private final ConfiguredRetryService retryService;
    private final RetryMetricsListener metricsListener;
    
    @Autowired
    public ConfiguredRetryRunner(
            ConfiguredRetryService retryService, 
            RetryMetricsListener metricsListener) {
        this.retryService = retryService;
        this.metricsListener = metricsListener;
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("===== 開始基於配置的重試示範 =====");
        
        // 執行資料庫操作示例
        System.out.println("\n[示範資料庫重試]");
        try {
            String result = retryService.performDatabaseOperation("SELECT * FROM users");
            System.out.println("操作成功: " + result);
        } catch (Exception e) {
            System.err.println("資料庫操作最終失敗: " + e.getMessage());
        }
        
        System.out.println("\n-----------------------------------\n");
        
        // 執行網絡操作示例
        System.out.println("\n[示範網絡重試]");
        try {
            String result = retryService.performNetworkOperation("https://api.example.com/data");
            System.out.println("操作成功: " + result);
        } catch (Exception e) {
            System.err.println("網絡操作最終失敗: " + e.getMessage());
        }
        
        System.out.println("\n-----------------------------------\n");
        
        // 執行受監控的操作示例
        System.out.println("\n[示範監控重試]");
        for (int i = 1; i <= 3; i++) {
            try {
                String result = retryService.performMonitoredOperation("task-" + i);
                System.out.println("操作成功: " + result);
            } catch (Exception e) {
                System.err.println("監控操作最終失敗: " + e.getMessage());
            }
        }
        
        // 打印度量信息
        metricsListener.printStats();
        
        System.out.println("===== 基於配置的重試示範結束 =====");
    }
}
