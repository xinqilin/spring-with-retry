package com.bill.springretry.programmatic;

import com.bill.springretry.exception.DatabaseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * @author Bill.Lin 2025/4/13
 * 演示編程式重試的運行器
 */
@Component
@Profile("programmatic")
public class ProgrammaticRetryRunner implements CommandLineRunner {

    private final ProgrammaticRetryService retryService;
    
    @Autowired
    public ProgrammaticRetryRunner(ProgrammaticRetryService retryService) {
        this.retryService = retryService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        // 演示簡單重試
        try {
            String result = retryService.executeWithRetry(context -> {
                System.out.println("執行簡單重試操作 - 嘗試 #" + context.getRetryCount());
                
                // 模擬失敗概率
                if (Math.random() < 0.8) {
                    throw new RuntimeException("簡單重試操作失敗");
                }
                
                return "簡單重試操作成功!";
            });
            
            System.out.println("結果: " + result);
        } catch (Exception e) {
            System.err.println("簡單重試最終失敗: " + e.getMessage());
        }
        
        System.out.println("\n-----------------------------------\n");
        
        // 演示自訂異常分類重試
        try {
            String result = retryService.executeWithCustomRetry(context -> {
                System.out.println("執行自訂分類重試操作 - 嘗試 #" + context.getRetryCount());
                
                // 模擬數據庫異常
                if (Math.random() < 0.8) {
                    throw new DatabaseException("數據庫連接失敗");
                }
                
                return "自訂分類重試操作成功!";
            });
            
            System.out.println("結果: " + result);
        } catch (Exception e) {
            System.err.println("自訂分類重試最終失敗: " + e.getMessage());
        }
        
        System.out.println("\n-----------------------------------\n");
        
        // 演示使用上下文的重試
        try {
            String result = retryService.executeWithContext(context -> {
                System.out.println("執行帶上下文的重試操作 - 嘗試 #" + context.getRetryCount());
                
                // 這裡可以訪問上下文中的屬性
                long startTime = (long) context.getAttribute("startTime");
                System.out.println("距離開始已經過去: " + (System.currentTimeMillis() - startTime) + "ms");
                
                // 前兩次嘗試失敗，第三次成功
                if (context.getRetryCount() < 2) {
                    throw new RuntimeException("故意的失敗，嘗試次數: " + context.getRetryCount());
                }
                
                return "帶上下文的重試操作成功!";
            });
            
            System.out.println("結果: " + result);
        } catch (Exception e) {
            System.err.println("帶上下文的重試最終失敗: " + e.getMessage());
        }
    }
}
