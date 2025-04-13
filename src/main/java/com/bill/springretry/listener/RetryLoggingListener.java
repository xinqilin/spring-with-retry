package com.bill.springretry.listener;

import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

import java.util.Date;

/**
 * @author Bill.Lin 2025/4/13
 * 重試監聽器 - 記錄所有重試相關事件
 */
public class RetryLoggingListener implements RetryListener {

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        // 在重試開始前調用
        System.out.println("[" + new Date() + "] 重試操作開始: " + getOperationName(context));
        
        // 在上下文中添加開始時間
        context.setAttribute("startTime", System.currentTimeMillis());
        
        // 返回 true 允許重試繼續，返回 false 會阻止重試
        return true;
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // 在重試結束後調用（無論成功或失敗）
        long startTime = context.getAttribute("startTime") != null ? 
                (long) context.getAttribute("startTime") : 0;
        long duration = System.currentTimeMillis() - startTime;
        
        if (throwable == null) {
            System.out.println("[" + new Date() + "] 重試操作成功完成: " + 
                    getOperationName(context) + ", 總嘗試次數: " + 
                    (context.getRetryCount() + 1) + ", 耗時: " + duration + "ms");
        } else {
            System.err.println("[" + new Date() + "] 重試操作最終失敗: " + 
                    getOperationName(context) + ", 總嘗試次數: " + 
                    (context.getRetryCount() + 1) + ", 耗時: " + duration + 
                    "ms, 錯誤: " + throwable.getMessage());
        }
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // 在每次嘗試發生錯誤時調用
        long startTime = context.getAttribute("startTime") != null ? 
                (long) context.getAttribute("startTime") : 0;
        long duration = System.currentTimeMillis() - startTime;
        
        System.err.println("[" + new Date() + "] 重試嘗試 #" + context.getRetryCount() + 
                " 失敗: " + getOperationName(context) + ", 已耗時: " + duration + 
                "ms, 錯誤: " + throwable.getMessage());
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
        return "未命名操作";
    }
}
