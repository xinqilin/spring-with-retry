package com.bill.springretry.custom;

import org.junit.jupiter.api.Test;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.BackOffContext;
import org.springframework.retry.context.RetryContextSupport;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TimeAwareBackOffPolicyTest {

    @Test
    void testBackOff_NormalTime() throws Exception {
        // 創建一個模擬的時間感知退避策略，使用自定義的時間提供器
        LocalTime normalTime = LocalTime.of(14, 0); // 下午2點，正常時段
        TimeAwareBackOffPolicy policy = createPolicyWithMockTime(normalTime);
        
        // 創建模擬的重試上下文
        RetryContext context = new RetryContextSupport(null);
        
        // 獲取退避上下文
        BackOffContext backOffContext = policy.start(context);
        
        // 執行退避並測量時間
        long startTime = System.currentTimeMillis();
        policy.backOff(backOffContext);
        long endTime = System.currentTimeMillis();
        
        // 計算實際退避時間
        long actualDelay = endTime - startTime;
        
        // 驗證退避時間接近基本間隔 (1000ms)
        assertTrue(actualDelay >= 950 && actualDelay <= 1050, 
                   "正常時段的退避時間應該接近1000ms，實際為：" + actualDelay);
    }

    @Test
    void testBackOff_PeakTime() throws Exception {
        // 創建一個模擬的時間感知退避策略，使用自定義的時間提供器
        LocalTime peakTime = LocalTime.of(12, 0); // 中午12點，高峰時段
        TimeAwareBackOffPolicy policy = createPolicyWithMockTime(peakTime);
        
        // 創建模擬的重試上下文
        RetryContext context = new RetryContextSupport(null);
        
        // 獲取退避上下文
        BackOffContext backOffContext = policy.start(context);
        
        // 執行退避並測量時間
        long startTime = System.currentTimeMillis();
        policy.backOff(backOffContext);
        long endTime = System.currentTimeMillis();
        
        // 計算實際退避時間
        long actualDelay = endTime - startTime;
        
        // 驗證退避時間接近高峰時段間隔 (2000ms，假設倍數為2)
        assertTrue(actualDelay >= 1950 && actualDelay <= 2050, 
                   "高峰時段的退避時間應該接近2000ms，實際為：" + actualDelay);
    }

    @Test
    void testBackOff_NightTime() throws Exception {
        // 創建一個模擬的時間感知退避策略，使用自定義的時間提供器
        LocalTime nightTime = LocalTime.of(2, 0); // 凌晨2點，夜間時段
        TimeAwareBackOffPolicy policy = createPolicyWithMockTime(nightTime);
        
        // 創建模擬的重試上下文
        RetryContext context = new RetryContextSupport(null);
        
        // 獲取退避上下文
        BackOffContext backOffContext = policy.start(context);
        
        // 執行退避並測量時間
        long startTime = System.currentTimeMillis();
        policy.backOff(backOffContext);
        long endTime = System.currentTimeMillis();
        
        // 計算實際退避時間
        long actualDelay = endTime - startTime;
        
        // 驗證退避時間接近夜間間隔 (500ms)
        assertTrue(actualDelay >= 450 && actualDelay <= 550, 
                   "夜間時段的退避時間應該接近500ms，實際為：" + actualDelay);
    }

    @Test
    void testBackOff_InterruptedException() {
        // 創建一個會被中斷的線程
        Thread thread = new Thread(() -> {
            TimeAwareBackOffPolicy policy = new TimeAwareBackOffPolicy(10000, 10000, 9, 18, 2.0f);
            RetryContext context = new RetryContextSupport(null);
            BackOffContext backOffContext = policy.start(context);
            
            try {
                // 嘗試退避
                policy.backOff(backOffContext);
                fail("應該拋出BackOffInterruptedException");
            } catch (Exception e) {
                assertTrue(e instanceof org.springframework.retry.backoff.BackOffInterruptedException, 
                           "應該拋出BackOffInterruptedException");
                assertTrue(e.getCause() instanceof InterruptedException, 
                           "原因應該是InterruptedException");
            }
        });
        
        // 啟動線程並立即中斷它
        thread.start();
        thread.interrupt();
        
        // 等待線程完成
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            fail("等待線程時發生中斷");
        }
    }

    private TimeAwareBackOffPolicy createPolicyWithMockTime(LocalTime fixedTime) {
        // 建立一個有自定義當前時間的策略實例
        // 注意：這需要修改 TimeAwareBackOffPolicy 實現以支持注入時間提供器
        
        // 實際測試中，你可能需要使用反射或修改原始類來實現這個功能
        // 以下是一個簡單的實現示例
        return new TimeAwareBackOffPolicy() {
            @Override
            protected LocalTime getCurrentTime() {
                return fixedTime;
            }
        };
    }
}
