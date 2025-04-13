package com.bill.springretry.custom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * @author Bill.Lin 2025/4/13
 * 自定義重試策略的 runner
 */
@Component
@Profile("custom")
public class CustomRetryRunner implements CommandLineRunner {

    private final CustomRetryService retryService;
    
    @Autowired
    public CustomRetryRunner(CustomRetryService retryService) {
        this.retryService = retryService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("===== 開始自定義重試策略示範 =====");
        
        // 測試 HTTP 重試策略 - 使用可重試的狀態碼但最終失敗
        System.out.println("\n[測試 HTTP 重試策略 - 503 錯誤，最終失敗]");
        try {
            retryService.simulateHttpRequest(503, -1); // 始終返回 503，永不成功
        } catch (Exception e) {
            System.out.println("預期的異常捕獲: " + e.getMessage());
        }
        
        System.out.println("\n-----------------------------------\n");
        
        // 測試 HTTP 重試策略 - 使用可重試的狀態碼並在第 3 次嘗試後成功
        System.out.println("\n[測試 HTTP 重試策略 - 503 錯誤，第 3 次後成功]");
        try {
            retryService.simulateHttpRequest(503, 2); // 返回 503，在第 3 次嘗試後成功
        } catch (Exception e) {
            System.err.println("非預期的異常: " + e.getMessage());
        }
        
        System.out.println("\n-----------------------------------\n");
        
        // 測試 HTTP 重試策略 - 使用不可重試的狀態碼
        System.out.println("\n[測試 HTTP 重試策略 - 400 錯誤，不應該重試]");
        try {
            retryService.simulateHttpRequest(400, -1); // 返回 400，不應該重試
        } catch (Exception e) {
            System.out.println("預期的異常捕獲: " + e.getMessage());
        }
        
        System.out.println("\n-----------------------------------\n");
        
        // 測試時間感知退避策略
        System.out.println("\n[測試時間感知退避策略]");
        try {
            retryService.executeTimeAwareOperation(context -> {
                System.out.println("執行時間感知操作，嘗試 #" + (context.getRetryCount() + 1));
                
                // 前兩次嘗試失敗，第三次成功
                if (context.getRetryCount() < 2) {
                    throw new RuntimeException("故意的失敗");
                }
                
                return "時間感知操作成功!";
            });
        } catch (Exception e) {
            System.err.println("非預期的異常: " + e.getMessage());
        }
        
        System.out.println("===== 自定義重試策略示範結束 =====");
    }
}
