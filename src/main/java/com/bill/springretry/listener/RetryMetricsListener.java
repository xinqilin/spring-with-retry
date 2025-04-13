package com.bill.springretry.listener;

import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Bill.Lin 2025/4/13
 * 重試度量監聽器 - 收集重試相關的統計信息
 */
public class RetryMetricsListener extends RetryListenerSupport {

    // 累計統計
    private final AtomicInteger totalOperations = new AtomicInteger(0);
    private final AtomicInteger successfulOperations = new AtomicInteger(0);
    private final AtomicInteger failedOperations = new AtomicInteger(0);
    private final AtomicLong totalRetryAttempts = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    
    // 按操作類型分類的統計
    private final Map<String, OperationStats> statsByOperation = new ConcurrentHashMap<>();
    
    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        // 增加總操作數
        totalOperations.incrementAndGet();
        
        // 記錄開始時間
        context.setAttribute("metricStartTime", System.currentTimeMillis());
        
        // 獲取操作名稱
        String operationName = getOperationName(context);
        
        // 獲取或創建該操作的統計對象
        OperationStats stats = statsByOperation.computeIfAbsent(
                operationName, name -> new OperationStats());
        
        // 增加該操作的總次數
        stats.totalCount.incrementAndGet();
        
        return true;
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // 計算執行時間
        long startTime = (long) context.getAttribute("metricStartTime");
        long duration = System.currentTimeMillis() - startTime;
        
        // 累計總執行時間
        totalExecutionTime.addAndGet(duration);
        
        // 獲取操作名稱
        String operationName = getOperationName(context);
        OperationStats stats = statsByOperation.get(operationName);
        
        // 更新統計信息
        if (throwable == null) {
            // 操作最終成功
            successfulOperations.incrementAndGet();
            stats.successCount.incrementAndGet();
        } else {
            // 操作最終失敗
            failedOperations.incrementAndGet();
            stats.failureCount.incrementAndGet();
        }
        
        // 累計重試次數
        int retryCount = context.getRetryCount();
        totalRetryAttempts.addAndGet(retryCount);
        stats.totalRetryAttempts.addAndGet(retryCount);
        
        // 累計執行時間
        stats.totalExecutionTime.addAndGet(duration);
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // 所有錯誤都會計入總重試次數，這個在 close 方法中已經處理
    }
    
    /**
     * 嘗試從上下文獲取操作名稱
     */
    private String getOperationName(RetryContext context) {
        // 如果在上下文中設置了操作名稱，則使用它
        if (context.getAttribute("operationName") != null) {
            return (String) context.getAttribute("operationName");
        }
        
        // 否則返回默認名稱
        return "DEFAULT";
    }
    
    /**
     * 打印當前統計信息
     */
    public void printStats() {
        System.out.println("\n===== 重試統計信息 =====");
        System.out.println("總操作數: " + totalOperations.get());
        System.out.println("成功操作數: " + successfulOperations.get());
        System.out.println("失敗操作數: " + failedOperations.get());
        System.out.println("總重試次數: " + totalRetryAttempts.get());
        
        double avgExecutionTime = totalOperations.get() > 0 
                ? (double) totalExecutionTime.get() / totalOperations.get() 
                : 0;
        System.out.println("平均執行時間: " + String.format("%.2f", avgExecutionTime) + "ms");
        
        System.out.println("\n按操作類型的統計信息:");
        statsByOperation.forEach((operation, stats) -> {
            System.out.println("\n操作: " + operation);
            System.out.println("  總次數: " + stats.totalCount.get());
            System.out.println("  成功次數: " + stats.successCount.get());
            System.out.println("  失敗次數: " + stats.failureCount.get());
            System.out.println("  總重試次數: " + stats.totalRetryAttempts.get());
            
            double avgTime = stats.totalCount.get() > 0 
                    ? (double) stats.totalExecutionTime.get() / stats.totalCount.get() 
                    : 0;
            System.out.println("  平均執行時間: " + String.format("%.2f", avgTime) + "ms");
            
            double successRate = stats.totalCount.get() > 0 
                    ? (double) stats.successCount.get() / stats.totalCount.get() * 100 
                    : 0;
            System.out.println("  成功率: " + String.format("%.2f", successRate) + "%");
        });
        
        System.out.println("\n=========================");
    }
    
    /**
     * 每種操作的統計信息
     */
    private static class OperationStats {
        final AtomicInteger totalCount = new AtomicInteger(0);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        final AtomicLong totalRetryAttempts = new AtomicLong(0);
        final AtomicLong totalExecutionTime = new AtomicLong(0);
    }
}
