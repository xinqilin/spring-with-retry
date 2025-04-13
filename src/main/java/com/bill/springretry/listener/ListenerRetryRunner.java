package com.bill.springretry.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * @author Bill.Lin 2025/4/13
 * 演示重試監聽器的運行器
 */
@Component
@Profile("listener")
public class ListenerRetryRunner implements CommandLineRunner {

    private final ListenerRetryService retryService;
    
    @Autowired
    public ListenerRetryRunner(ListenerRetryService retryService) {
        this.retryService = retryService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("===== 開始重試監聽器示範 =====");
        
        retryService.simulateOperations();
        
        System.out.println("===== 重試監聽器示範結束 =====");
    }
}
