package com.bill.springretry.stateful;

import com.bill.springretry.exception.DatabaseException;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryState;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.DefaultRetryState;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Bill.Lin 2025/4/13
 * 示範有狀態的重試機制 - 重試狀態可以在多次調用之間保持
 */
@Service
public class StatefulRetryService {

    // 用於在服務中存儲重試狀態
    private final Map<String, RetryState> retryStateMap = new ConcurrentHashMap<>();
    private final RetryTemplate retryTemplate;
    
    // 模擬一個資料庫或緩存
    private final Map<String, TransactionData> dataStore = new ConcurrentHashMap<>();
    
    public StatefulRetryService() {
        this.retryTemplate = new RetryTemplate();
        
        // 配置重試政策
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(DatabaseException.class, true);
        
        SimpleRetryPolicy policy = new SimpleRetryPolicy(3, retryableExceptions);
        retryTemplate.setRetryPolicy(policy);
    }
    
    /**
     * 處理一個事務，支持有狀態的重試
     * 如果操作失敗，重試狀態會被保存，下一次調用時可以繼續重試
     */
    public String processTransaction(String transactionId, String data) throws Exception {
        // 如果沒有指定交易ID，則創建一個新的
        if (transactionId == null || transactionId.isEmpty()) {
            transactionId = UUID.randomUUID().toString();
        }
        
        // 獲取或創建重試狀態
        RetryState retryState = retryStateMap.computeIfAbsent(
                transactionId, 
                id -> new DefaultRetryState(id, true)
        );
        
        // 創建交易數據對象
        final TransactionData transactionData = new TransactionData(transactionId, data);
        
        try {
            // 執行有狀態的重試
            return retryTemplate.execute(
                    // 重試回調
                    (RetryCallback<String, Exception>) context -> {
                        System.out.println("處理交易 " + transactionId + ", 嘗試 #" + (context.getRetryCount() + 1));
                        
                        // 模擬一個可能失敗的操作
                        if (!isTransactionValid(transactionData) || Math.random() < 0.7) {
                            throw new DatabaseException("處理交易 " + transactionId + " 時發生數據庫錯誤");
                        }
                        
                        // 成功處理交易
                        dataStore.put(transactionId, transactionData);
                        return "交易 " + transactionId + " 處理成功";
                    },
                    // 恢復回調
                    (RecoveryCallback<String>) context -> {
                        System.err.println("交易 " + transactionId + " 處理失敗，執行恢復操作");
                        
                        // 清理重試狀態
                        retryStateMap.remove(transactionId);
                        
                        // 返回恢復結果
                        return "交易 " + transactionId + " 恢復處理: " + context.getLastThrowable().getMessage();
                    },
                    // 使用有狀態的重試
                    retryState
            );
        } catch (Exception e) {
            System.err.println("交易處理異常: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * 檢查交易是否有效
     */
    private boolean isTransactionValid(TransactionData data) {
        // 模擬一些驗證邏輯
        return data != null && data.getData() != null && !data.getData().isEmpty();
    }
    
    /**
     * 檢查交易的重試狀態
     */
    public boolean hasActiveRetryState(String transactionId) {
        return retryStateMap.containsKey(transactionId);
    }
    
    /**
     * 清除指定交易的重試狀態
     */
    public void clearRetryState(String transactionId) {
        retryStateMap.remove(transactionId);
    }
    
    /**
     * 清除所有重試狀態
     */
    public void clearAllRetryStates() {
        retryStateMap.clear();
    }
    
    /**
     * 獲取當前活動的重試狀態數量
     */
    public int getActiveRetryStateCount() {
        return retryStateMap.size();
    }
    
    /**
     * 表示一個交易數據的內部類
     */
    static class TransactionData {
        private final String id;
        private final String data;
        
        public TransactionData(String id, String data) {
            this.id = id;
            this.data = data;
        }
        
        public String getId() {
            return id;
        }
        
        public String getData() {
            return data;
        }
    }
}
