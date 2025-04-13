package com.bill.springretry.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RetryMetricsListenerTest {

    private RetryMetricsListener metricsListener;
    private RetryTemplate retryTemplate;
    
    @BeforeEach
    void setUp() {
        // 創建新的監聽器和重試模板
        metricsListener = new RetryMetricsListener();
        
        retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3); // 最多重試3次
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.registerListener(metricsListener);
    }

    @Test
    void testSuccessfulOperation() throws Exception {
        // 執行立即成功的操作
        String result = retryTemplate.execute(context -> {
            context.setAttribute("operationName", "testSuccess");
            return "成功";
        });
        
        assertEquals("成功", result);
        
        // 捕獲標準輸出以檢查指標輸出
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        
        try {
            // 打印指標
            metricsListener.printStats();
            
            // 驗證指標
            String output = outContent.toString();
            assertTrue(output.contains("總操作數: 1"));
            assertTrue(output.contains("成功操作數: 1"));
            assertTrue(output.contains("失敗操作數: 0"));
            assertTrue(output.contains("總重試次數: 0")); // 不需要重試
            assertTrue(output.contains("操作: testSuccess"));
            assertTrue(output.contains("成功率: 100.00%"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testFailedOperation() throws Exception {
        // 執行始終失敗的操作
        Exception exception = assertThrows(RuntimeException.class, () -> {
            retryTemplate.execute(context -> {
                context.setAttribute("operationName", "testFailure");
                throw new RuntimeException("測試失敗");
            });
        });
        
        // 捕獲標準輸出以檢查指標輸出
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        
        try {
            // 打印指標
            metricsListener.printStats();
            
            // 驗證指標
            String output = outContent.toString();
            assertTrue(output.contains("總操作數: 1"));
            assertTrue(output.contains("成功操作數: 0"));
            assertTrue(output.contains("失敗操作數: 1"));
            assertTrue(output.contains("總重試次數: 2")); // 原始嘗試 + 2次重試 = 3次嘗試
            assertTrue(output.contains("操作: testFailure"));
            assertTrue(output.contains("成功率: 0.00%"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testEventuallySuccessfulOperation() throws Exception {
        // 創建一個計數器以跟踪嘗試次數
        final int[] attempts = {0};
        
        // 執行最終成功的操作
        String result = retryTemplate.execute(context -> {
            context.setAttribute("operationName", "testEventualSuccess");
            attempts[0]++;
            
            if (attempts[0] <= 2) {
                throw new RuntimeException("測試失敗，嘗試 #" + attempts[0]);
            }
            
            return "最終成功";
        });
        
        assertEquals("最終成功", result);
        assertEquals(3, attempts[0]);
        
        // 捕獲標準輸出以檢查指標輸出
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        
        try {
            // 打印指標
            metricsListener.printStats();
            
            // 驗證指標
            String output = outContent.toString();
            assertTrue(output.contains("總操作數: 1"));
            assertTrue(output.contains("成功操作數: 1"));
            assertTrue(output.contains("失敗操作數: 0"));
            assertTrue(output.contains("總重試次數: 2")); // 2次失敗後成功
            assertTrue(output.contains("操作: testEventualSuccess"));
            assertTrue(output.contains("成功率: 100.00%"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testMultipleOperations() throws Exception {
        // 執行多種不同的操作
        Map<String, Integer> attemptsMap = new HashMap<>();
        attemptsMap.put("op1", 0);
        attemptsMap.put("op2", 0);
        attemptsMap.put("op3", 0);
        
        // 操作1: 立即成功
        retryTemplate.execute(context -> {
            context.setAttribute("operationName", "op1");
            attemptsMap.put("op1", attemptsMap.get("op1") + 1);
            return "操作1成功";
        });
        
        // 操作2: 始終失敗
        try {
            retryTemplate.execute(context -> {
                context.setAttribute("operationName", "op2");
                attemptsMap.put("op2", attemptsMap.get("op2") + 1);
                throw new RuntimeException("操作2失敗");
            });
        } catch (Exception e) {
            // 預期的異常
        }
        
        // 操作3: 最終成功
        retryTemplate.execute(context -> {
            context.setAttribute("operationName", "op3");
            int attempts = attemptsMap.get("op3") + 1;
            attemptsMap.put("op3", attempts);
            
            if (attempts <= 1) {
                throw new RuntimeException("操作3暫時失敗");
            }
            
            return "操作3最終成功";
        });
        
        // 捕獲標準輸出以檢查指標輸出
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        
        try {
            // 打印指標
            metricsListener.printStats();
            
            // 驗證指標
            String output = outContent.toString();
            assertTrue(output.contains("總操作數: 3"));
            assertTrue(output.contains("成功操作數: 2"));
            assertTrue(output.contains("失敗操作數: 1"));
            
            assertTrue(output.contains("操作: op1"));
            assertTrue(output.contains("操作: op2"));
            assertTrue(output.contains("操作: op3"));
        } finally {
            System.setOut(originalOut);
        }
    }
}
