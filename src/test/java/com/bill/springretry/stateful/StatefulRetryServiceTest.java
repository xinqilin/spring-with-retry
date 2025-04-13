package com.bill.springretry.stateful;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StatefulRetryServiceTest {

    @Autowired
    private StatefulRetryService retryService;
    
    // 每個測試後清理所有重試狀態
    @AfterEach
    void cleanup() {
        retryService.clearAllRetryStates();
    }

    @Test
    void testProcessTransaction_Success() throws Exception {
        // 創建一個事務ID
        String transactionId = UUID.randomUUID().toString();
        
        // 設置重定向標準輸出以檢查輸出
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        
        try {
            // 使服務始終成功處理事務 (透過反射或其他方式)
            // 注意：這裡我們假設 StatefulRetryService 有一個方法可以控制健康狀態或測試模式
            // retryService.setTestMode(true);
            
            // 處理事務
            String result = retryService.processTransaction(transactionId, "測試資料");
            
            // 驗證結果
            assertTrue(result.contains("交易 " + transactionId + " 處理成功"));
            assertFalse(retryService.hasActiveRetryState(transactionId), "不應該有活動的重試狀態");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testProcessTransaction_FailureAndRetry() throws Exception {
        // 創建一個事務ID
        String transactionId = UUID.randomUUID().toString();
        
        // 設置重定向標準錯誤輸出以檢查輸出
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errContent));
        
        try {
            // 使服務總是失敗，以模擬錯誤情況 (透過反射或其他方式)
            // 注意：這裡我們假設 StatefulRetryService 有一個方法可以控制健康狀態或測試模式
            // retryService.setTestMode(false);
            
            // 第一次處理事務
            Exception exception = assertThrows(Exception.class, () -> {
                retryService.processTransaction(transactionId, "測試錯誤資料");
            });
            
            // 驗證異常
            assertTrue(exception.getMessage().contains("交易處理異常") || 
                       exception.getMessage().contains("數據庫錯誤"));
            
            // 驗證重試狀態
            assertTrue(retryService.hasActiveRetryState(transactionId), "應該有活動的重試狀態");
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void testClearRetryState() throws Exception {
        // 創建一個事務ID
        String transactionId = UUID.randomUUID().toString();
        
        try {
            // 嘗試處理事務，預期會失敗並創建重試狀態
            assertThrows(Exception.class, () -> {
                retryService.processTransaction(transactionId, "測試清除資料");
            });
            
            // 驗證已創建重試狀態
            assertTrue(retryService.hasActiveRetryState(transactionId), "應該有活動的重試狀態");
            
            // 清除特定的重試狀態
            retryService.clearRetryState(transactionId);
            
            // 驗證狀態已清除
            assertFalse(retryService.hasActiveRetryState(transactionId), "不應該有活動的重試狀態");
        } catch (Exception e) {
            // 如果處理事務時出現異常，這是預期的行為
        }
    }

    @Test
    void testClearAllRetryStates() throws Exception {
        // 創建多個事務ID
        String transactionId1 = UUID.randomUUID().toString();
        String transactionId2 = UUID.randomUUID().toString();
        
        try {
            // 嘗試處理多個事務，預期會失敗並創建重試狀態
            assertThrows(Exception.class, () -> {
                retryService.processTransaction(transactionId1, "測試清除所有資料1");
            });
            
            assertThrows(Exception.class, () -> {
                retryService.processTransaction(transactionId2, "測試清除所有資料2");
            });
            
            // 驗證已創建多個重試狀態
            assertTrue(retryService.hasActiveRetryState(transactionId1), "事務1應該有活動的重試狀態");
            assertTrue(retryService.hasActiveRetryState(transactionId2), "事務2應該有活動的重試狀態");
            
            // 清除所有重試狀態
            retryService.clearAllRetryStates();
            
            // 驗證所有狀態已清除
            assertFalse(retryService.hasActiveRetryState(transactionId1), "事務1不應該有活動的重試狀態");
            assertFalse(retryService.hasActiveRetryState(transactionId2), "事務2不應該有活動的重試狀態");
            assertEquals(0, retryService.getActiveRetryStateCount(), "活動重試狀態數量應該為0");
        } catch (Exception e) {
            // 如果處理事務時出現異常，這是預期的行為
        }
    }
}
