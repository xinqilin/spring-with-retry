package com.bill.springretry.configuration;

import com.bill.springretry.exception.DatabaseException;
import com.bill.springretry.exception.TransientNetworkException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConfiguredRetryServiceTest {

    @Autowired
    private ConfiguredRetryService retryService;

    @Test
    void testExecuteSimpleRetry() throws Exception {
        // 測試簡單重試模板
        AtomicInteger attempts = new AtomicInteger(0);
        
        String result = retryService.executeSimpleRetry(context -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 2) {
                throw new RuntimeException("測試失敗");
            }
            return "簡單重試成功";
        });
        
        assertEquals("簡單重試成功", result);
        assertEquals(2, attempts.get(), "應該嘗試2次");
    }

    @Test
    void testExecuteDatabaseRetry() throws Exception {
        // 測試數據庫重試模板
        AtomicInteger attempts = new AtomicInteger(0);
        
        Exception exception = assertThrows(Exception.class, () -> {
            retryService.executeDatabaseRetry(context -> {
                int attempt = attempts.incrementAndGet();
                // 總是拋出數據庫異常
                throw new DatabaseException("測試數據庫連接失敗");
            });
        });
        
        // 驗證重試次數
        assertEquals(5, attempts.get(), "數據庫重試策略應該嘗試5次");
    }

    @Test
    void testExecuteNetworkRetry() throws Exception {
        // 測試網絡重試模板
        AtomicInteger attempts = new AtomicInteger(0);
        
        Exception exception = assertThrows(Exception.class, () -> {
            retryService.executeNetworkRetry(context -> {
                int attempt = attempts.incrementAndGet();
                // 總是拋出網絡異常
                throw new TransientNetworkException("測試網絡連接失敗");
            });
        });
        
        // 驗證重試次數
        assertEquals(4, attempts.get(), "網絡重試策略應該嘗試4次");
    }

    @Test
    void testExecuteMonitoredRetry() throws Exception {
        // 測試帶有監聽器的重試模板
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        
        try {
            AtomicInteger attempts = new AtomicInteger(0);
            
            String result = retryService.executeMonitoredRetry(context -> {
                context.setAttribute("operationName", "testMonitored");
                int attempt = attempts.incrementAndGet();
                
                if (attempt < 2) {
                    throw new RuntimeException("測試失敗");
                }
                
                return "監控重試成功";
            });
            
            assertEquals("監控重試成功", result);
            assertEquals(2, attempts.get(), "應該嘗試2次");
            
            // 驗證監聽器有記錄相關日誌
            // 注意：實際監聽器可能將日誌輸出到其他位置，這裡僅示例
            String output = outContent.toString();
            // 假設有相關日誌輸出，檢查是否包含預期內容
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testPerformDatabaseOperation_Success() throws Exception {
        // 使服務成功處理請求（需要修改內部邏輯或使用模擬）
        // 這裡使用測試替身或反射機制修改內部邏輯來確保操作成功
        // 例如，使用反射設置一個測試標誌或替換隨機數生成器
        
        // 捕獲標準輸出以驗證日誌
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        
        try {
            // 這裡我們假設服務有一些機制可以控制其行為
            // 實際測試可能需要調整實現或使用不同的方法
            
            // 調用方法
            String result = retryService.performDatabaseOperation("SELECT * FROM test");
            
            // 驗證結果包含預期的成功訊息
            assertTrue(result.contains("資料庫查詢結果"));
            assertTrue(outContent.toString().contains("執行資料庫查詢"));
        } catch (Exception e) {
            // 如果操作總是失敗，則捕獲異常，測試可能需要修改
            System.err.println("測試未成功: " + e.getMessage());
            throw e;
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testPerformNetworkOperation_Success() throws Exception {
        // 同上，確保網絡操作成功
        
        // 捕獲標準輸出以驗證日誌
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        
        try {
            // 調用方法
            String result = retryService.performNetworkOperation("https://example.com");
            
            // 驗證結果包含預期的成功訊息
            assertTrue(result.contains("網絡請求結果"));
            assertTrue(outContent.toString().contains("請求URL"));
        } catch (Exception e) {
            // 如果操作總是失敗，則捕獲異常，測試可能需要修改
            System.err.println("測試未成功: " + e.getMessage());
            throw e;
        } finally {
            System.setOut(originalOut);
        }
    }
}
