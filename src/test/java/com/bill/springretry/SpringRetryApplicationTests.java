package com.bill.springretry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootTest
@EnableRetry
class SpringRetryApplicationTests {

    @Test
    void contextLoads() {
        // 測試應用程序上下文是否成功加載
    }

}
