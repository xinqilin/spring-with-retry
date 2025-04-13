package com.bill.springretry.service;

import com.bill.springretry.exception.TransientNetworkException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.retry.annotation.EnableRetry;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@EnableRetry
class ExternalServiceTest {

    @Spy
    @InjectMocks
    private ExternalService externalService;

    @Test
    void testCallExternalService_Success() {
        // 模擬成功的情況 (Math.random() 返回一個小於 0.7 的值，不拋出異常)
        doReturn(0.8).when(externalService).getRandomValue();

        // 捕獲標準輸出以驗證打印結果
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            // 執行測試
            externalService.callExternalService();

            // 驗證結果
            assertTrue(outContent.toString().contains("外部服務呼叫成功"));
        } finally {
            // 恢復標準輸出
            System.setOut(originalOut);
        }
    }

    @Test
    void testCallExternalService_FailureAndRecover() {
        // 模擬始終失敗的情況 (Math.random() 返回小於 0.7 的值，總是拋出異常)
        doReturn(0.5).when(externalService).getRandomValue();
        
        // 捕獲標準錯誤輸出以驗證恢復方法
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errContent));

        try {
            // 執行測試 - 應該失敗並呼叫恢復方法
            externalService.callExternalService();
            
            // 驗證結果 - 應該顯示恢復邏輯訊息
            assertTrue(errContent.toString().contains("所有重試失敗，執行恢復邏輯"));
        } finally {
            // 恢復標準錯誤輸出
            System.setErr(originalErr);
        }
    }
}
