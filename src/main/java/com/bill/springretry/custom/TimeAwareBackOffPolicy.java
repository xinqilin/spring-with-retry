package com.bill.springretry.custom;

import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.BackOffContext;
import org.springframework.retry.backoff.BackOffInterruptedException;
import org.springframework.retry.backoff.BackOffPolicy;

import java.time.LocalTime;

/**
 * @author Bill.Lin 2025/4/13
 * 自定義退避策略 - 根據當前時間調整退避間隔
 * 例如，高峰時段使用較長的退避時間，避免系統過載
 */
public class TimeAwareBackOffPolicy implements BackOffPolicy {

    private final long baseInterval;  // 基本退避間隔（毫秒）
    private final long nightInterval; // 夜間退避間隔（毫秒）
    private final int startHour;      // 高峰時段開始小時
    private final int endHour;        // 高峰時段結束小時
    private final float peakMultiplier; // 高峰時段倍數
    
    /**
     * 建立時間感知的退避策略
     * 
     * @param baseInterval 基本退避間隔（毫秒）
     * @param nightInterval 夜間退避間隔（毫秒）
     * @param startHour 高峰時段開始小時（24小時制）
     * @param endHour 高峰時段結束小時（24小時制）
     * @param peakMultiplier 高峰時段倍數
     */
    public TimeAwareBackOffPolicy(long baseInterval, long nightInterval, int startHour, int endHour, float peakMultiplier) {
        this.baseInterval = baseInterval;
        this.nightInterval = nightInterval;
        this.startHour = startHour;
        this.endHour = endHour;
        this.peakMultiplier = peakMultiplier;
    }
    
    /**
     * 使用默認值創建時間感知退避策略
     * 基本間隔為1秒，夜間間隔為500毫秒，高峰時段9點至18點，高峰時間退避時間翻倍
     */
    public TimeAwareBackOffPolicy() {
        this(1000, 500, 9, 18, 2.0f);
    }

    @Override
    public BackOffContext start(RetryContext context) {
        return new TimeAwareBackOffContext();
    }

    @Override
    public void backOff(BackOffContext backOffContext) throws BackOffInterruptedException {
        TimeAwareBackOffContext context = (TimeAwareBackOffContext) backOffContext;
        long sleepTime = calculateSleepTime();
        
        context.setLastBackOff(sleepTime);
        
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BackOffInterruptedException("Thread interrupted while sleeping", e);
        }
    }
    
    /**
     * 根據當前時間計算退避時間
     */
    private long calculateSleepTime() {
        LocalTime now = getCurrentTime();
        int currentHour = now.getHour();
        
        // 判斷是否在高峰時段
        if (currentHour >= startHour && currentHour < endHour) {
            System.out.println("目前是高峰時段，增加退避時間");
            return (long) (baseInterval * peakMultiplier);
        }
        
        // 判斷是否在夜間
        if (currentHour >= 22 || currentHour < 6) {
            System.out.println("目前是夜間時段，減少退避時間");
            return nightInterval;
        }
        
        // 默認使用基本退避時間
        System.out.println("目前是正常時段，使用標準退避時間");
        return baseInterval;
    }
    
    /**
     * 獲取當前時間 - 為了便於測試，可被重寫
     * @return 當前時間
     */
    protected LocalTime getCurrentTime() {
        return LocalTime.now();
    }
    
    /**
     * 自定義退避上下文
     */
    private static class TimeAwareBackOffContext implements BackOffContext {
        private long lastBackOff;
        
        public long getLastBackOff() {
            return lastBackOff;
        }
        
        public void setLastBackOff(long lastBackOff) {
            this.lastBackOff = lastBackOff;
        }
    }
}
