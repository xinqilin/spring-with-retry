package com.bill.springretry.circuitbreaker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * @author Bill.Lin 2025/4/13
 * 演示斷路器模式的運行器
 */
@Component
@Profile("circuitbreaker")
public class CircuitBreakerRunner implements CommandLineRunner {

    private final CircuitBreakerService circuitBreakerService;
    
    @Autowired
    public CircuitBreakerRunner(CircuitBreakerService circuitBreakerService) {
        this.circuitBreakerService = circuitBreakerService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("===== 開始斷路器模式示範 =====");
        
        // 步驟 1: 服務正常，進行幾次調用
        System.out.println("\n[步驟 1: 正常服務調用]");
        for (int i = 1; i <= 3; i++) {
            try {
                String result = circuitBreakerService.callExternalService("request-" + i);
                System.out.println("調用結果: " + result);
            } catch (Exception e) {
                System.err.println("調用異常: " + e.getMessage());
            }
            Thread.sleep(1000);
        }
        
        // 步驟 2: 將服務設為不健康，導致連續失敗
        System.out.println("\n[步驟 2: 服務不健康，連續失敗]");
        circuitBreakerService.setServiceHealthy(false);
        
        for (int i = 1; i <= 5; i++) {
            try {
                circuitBreakerService.callExternalService("unhealthy-request-" + i);
            } catch (Exception e) {
                System.err.println("預期的異常: " + e.getMessage());
            }
            Thread.sleep(1000);
        }
        
        // 步驟 3: 服務仍不健康，但斷路器應已打開，直接拒絕請求
        System.out.println("\n[步驟 3: 斷路器打開，拒絕請求]");
        for (int i = 1; i <= 3; i++) {
            try {
                circuitBreakerService.callExternalService("rejected-request-" + i);
            } catch (Exception e) {
                System.err.println("斷路器拒絕: " + e.getMessage());
            }
            Thread.sleep(1000);
        }
        
        // 步驟 4: 等待斷路器超時並半開，然後恢復服務健康
        System.out.println("\n[步驟 4: 等待斷路器重置，恢復服務]");
        System.out.println("等待斷路器重置時間...");
        Thread.sleep(10000); // 等待斷路器重置
        
        circuitBreakerService.setServiceHealthy(true);
        circuitBreakerService.resetCircuitBreaker();
        
        // 步驟 5: 服務恢復正常，斷路器應該關閉
        System.out.println("\n[步驟 5: 服務恢復正常，斷路器關閉]");
        for (int i = 1; i <= 3; i++) {
            try {
                String result = circuitBreakerService.callExternalService("recovered-request-" + i);
                System.out.println("調用結果: " + result);
            } catch (Exception e) {
                System.err.println("調用異常: " + e.getMessage());
            }
            Thread.sleep(1000);
        }
        
        System.out.println("===== 斷路器模式示範結束 =====");
    }
}
