package com.bill.springretry.stateful;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author Bill.Lin 2025/4/13
 * 示範有狀態重試的運行器
 */
@Component
@Profile("stateful")
public class StatefulRetryRunner implements CommandLineRunner {

    private final StatefulRetryService retryService;
    
    @Autowired
    public StatefulRetryRunner(StatefulRetryService retryService) {
        this.retryService = retryService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("===== 開始有狀態重試示範 =====");
        
        // 生成一個固定的交易ID，以便在多次調用間保持狀態
        String transactionId = UUID.randomUUID().toString();
        System.out.println("創建交易ID: " + transactionId);
        
        // 首次處理交易 - 可能會失敗並保存重試狀態
        try {
            String result = retryService.processTransaction(transactionId, "重要資料");
            System.out.println("交易處理結果: " + result);
        } catch (Exception e) {
            System.err.println("交易處理異常: " + e.getMessage());
        }
        
        // 檢查重試狀態
        boolean hasRetryState = retryService.hasActiveRetryState(transactionId);
        System.out.println("交易 " + transactionId + " 有活動的重試狀態: " + hasRetryState);
        
        System.out.println("\n-----------------------------------\n");
        
        // 如果有重試狀態，嘗試再次處理相同的交易
        if (hasRetryState) {
            System.out.println("嘗試再次處理相同的交易");
            try {
                String result = retryService.processTransaction(transactionId, "重要資料");
                System.out.println("第二次交易處理結果: " + result);
            } catch (Exception e) {
                System.err.println("第二次交易處理異常: " + e.getMessage());
            }
            
            // 再次檢查重試狀態
            hasRetryState = retryService.hasActiveRetryState(transactionId);
            System.out.println("處理後，交易 " + transactionId + " 有活動的重試狀態: " + hasRetryState);
        }
        
        System.out.println("\n-----------------------------------\n");
        
        // 創建一個新的交易，並故意保持重試狀態
        String newTransactionId = UUID.randomUUID().toString();
        System.out.println("創建新交易ID: " + newTransactionId);
        
        try {
            String result = retryService.processTransaction(newTransactionId, "新的資料");
            System.out.println("新交易處理結果: " + result);
        } catch (Exception e) {
            System.err.println("新交易處理異常: " + e.getMessage());
        }
        
        // 獲取當前活動的重試狀態數量
        int activeRetryStateCount = retryService.getActiveRetryStateCount();
        System.out.println("當前活動的重試狀態數量: " + activeRetryStateCount);
        
        // 清除所有重試狀態
        retryService.clearAllRetryStates();
        System.out.println("清除所有重試狀態...");
        
        // 檢查清除後的狀態
        activeRetryStateCount = retryService.getActiveRetryStateCount();
        System.out.println("清除後，活動的重試狀態數量: " + activeRetryStateCount);
        
        System.out.println("===== 有狀態重試示範結束 =====");
    }
}
