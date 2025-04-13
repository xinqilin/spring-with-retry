package com.bill.springretry.programmatic;

import com.bill.springretry.exception.DatabaseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProgrammaticRetryServiceTest {

    @Autowired
    private ProgrammaticRetryService retryService;

    @Test
    void testExecuteWithRetry_Success() throws Exception {
        // 測試一個最終成功的情況
        AtomicInteger attempts = new AtomicInteger(0);
        
        String result = retryService.executeWithRetry(context -> {
            attempts.incrementAndGet();
            if (attempts.get() <= 1) {
                // 第一次嘗試失敗
                throw new RuntimeException("測試失敗");
            }
            // 第二次嘗試成功
            return "成功";
        });
        
        assertEquals("成功", result);
        assertEquals(2, attempts.get(), "應該嘗試兩次");
    }

    @Test
    void testExecuteWithRetry_Failure() {
        // 測試一個始終失敗的情況
        AtomicInteger attempts = new AtomicInteger(0);
        
        Exception exception = assertThrows(Exception.class, () -> {
            retryService.executeWithRetry(context -> {
                attempts.incrementAndGet();
                throw new RuntimeException("始終失敗");
            });
        });
        
        assertTrue(exception.getMessage().contains("始終失敗"));
        assertEquals(3, attempts.get(), "應該嘗試三次 (重試策略設定的最大嘗試次數)");
    }

    @Test
    void testExecuteWithCustomRetry_DatabaseException() throws Exception {
        // 測試特定於資料庫異常的自定義重試
        AtomicInteger attempts = new AtomicInteger(0);
        
        Exception exception = assertThrows(Exception.class, () -> {
            retryService.executeWithCustomRetry(context -> {
                attempts.incrementAndGet();
                throw new DatabaseException("資料庫連接錯誤");
            });
        });
        
        assertTrue(exception.getMessage().contains("資料庫連接錯誤"));
        assertEquals(5, attempts.get(), "應該嘗試五次 (資料庫異常的最大嘗試次數)");
    }

    @Test
    void testExecuteWithContext() throws Exception {
        // 測試使用上下文的重試
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errContent));
        
        try {
            AtomicInteger attempts = new AtomicInteger(0);
            
            String result = retryService.executeWithContext(context -> {
                int attempt = attempts.incrementAndGet();
                if (attempt <= 2) {
                    throw new RuntimeException("測試失敗 #" + attempt);
                }
                return "使用上下文成功";
            });
            
            assertEquals("使用上下文成功", result);
            assertEquals(3, attempts.get(), "應該嘗試三次");
            
            // 驗證上下文信息被記錄
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("嘗試失敗"));
            assertTrue(errorOutput.contains("這是第 0 次重試") || errorOutput.contains("這是第 1 次重試"));
            assertTrue(errorOutput.contains("從開始到現在耗時"));
        } finally {
            System.setErr(originalErr);
        }
    }
}
