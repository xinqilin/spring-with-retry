package com.bill.springretry.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryLoggingListenerTest {

    private RetryLoggingListener loggingListener;
    private RetryTemplate retryTemplate;
    
    @BeforeEach
    void setUp() {
        // 創建新的監聽器和重試模板
        loggingListener = new RetryLoggingListener();
        
        retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3); // 最多重試3次
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.registerListener(loggingListener);
    }

    @Test
    void testLoggingOnSuccess() throws Exception {
        // 捕獲標準輸出
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        
        try {
            // 執行成功的操作
            String result = retryTemplate.execute(context -> {
                context.setAttribute("operationName", "testLoggingSuccess");
                return "成功";
            });
            
            assertEquals("成功", result);
            
            // 驗證日誌輸出
            String output = outContent.toString();
            assertTrue(output.contains("重試操作開始: testLoggingSuccess"));
            assertTrue(output.contains("重試操作成功完成: testLoggingSuccess"));
            assertTrue(output.contains("總嘗試次數: 1"));
            assertTrue(output.contains("耗時:"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testLoggingOnError() throws Exception {
        // 捕獲標準輸出和錯誤輸出
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        try {
            AtomicInteger attempts = new AtomicInteger(0);
            
            // 執行失敗的操作
            Exception exception = assertThrows(RuntimeException.class, () -> {
                retryTemplate.execute(context -> {
                    context.setAttribute("operationName", "testLoggingError");
                    attempts.incrementAndGet();
                    throw new RuntimeException("測試錯誤");
                });
            });
            
            assertEquals(3, attempts.get(), "應該嘗試3次");
            
            // 驗證日誌輸出
            String stdout = outContent.toString();
            String stderr = errContent.toString();
            
            assertTrue(stdout.contains("重試操作開始: testLoggingError"));
            assertTrue(stderr.contains("重試嘗試 #0 失敗: testLoggingError"));
            assertTrue(stderr.contains("重試嘗試 #1 失敗: testLoggingError"));
            assertTrue(stderr.contains("重試操作最終失敗: testLoggingError"));
            assertTrue(stderr.contains("總嘗試次數: 3"));
            assertTrue(stderr.contains("耗時:"));
            assertTrue(stderr.contains("錯誤: 測試錯誤"));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Test
    void testLoggingOnEventualSuccess() throws Exception {
        // 捕獲標準輸出和錯誤輸出
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        try {
            AtomicInteger attempts = new AtomicInteger(0);
            
            // 執行最終成功的操作（前兩次失敗，第三次成功）
            String result = retryTemplate.execute(context -> {
                context.setAttribute("operationName", "testLoggingEventualSuccess");
                int attempt = attempts.incrementAndGet();
                
                if (attempt < 3) {
                    throw new RuntimeException("測試失敗 #" + attempt);
                }
                
                return "最終成功";
            });
            
            assertEquals("最終成功", result);
            assertEquals(3, attempts.get(), "應該嘗試3次");
            
            // 驗證日誌輸出
            String stdout = outContent.toString();
            String stderr = errContent.toString();
            
            assertTrue(stdout.contains("重試操作開始: testLoggingEventualSuccess"));
            assertTrue(stderr.contains("重試嘗試 #0 失敗: testLoggingEventualSuccess"));
            assertTrue(stderr.contains("重試嘗試 #1 失敗: testLoggingEventualSuccess"));
            assertTrue(stdout.contains("重試操作成功完成: testLoggingEventualSuccess"));
            assertTrue(stdout.contains("總嘗試次數: 3"));
            assertTrue(stdout.contains("耗時:"));
            assertTrue(stderr.contains("錯誤: 測試失敗 #1"));
            assertTrue(stderr.contains("錯誤: 測試失敗 #2"));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Test
    void testMissingOperationName() throws Exception {
        // 捕獲標準輸出
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        
        try {
            // 執行沒有設置操作名稱的操作
            String result = retryTemplate.execute(context -> {
                // 故意不設置操作名稱
                return "無名操作";
            });
            
            assertEquals("無名操作", result);
            
            // 驗證日誌輸出使用了默認名稱
            String output = outContent.toString();
            assertTrue(output.contains("重試操作開始: 未命名操作"));
            assertTrue(output.contains("重試操作成功完成: 未命名操作"));
        } finally {
            System.setOut(originalOut);
        }
    }
}
