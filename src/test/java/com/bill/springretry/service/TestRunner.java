package com.bill.springretry.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author Bill.Lin 2025/4/13
 */
@Component
public class TestRunner implements CommandLineRunner {

    @Autowired
    private ExternalService externalService;

    @Override
    public void run(String... args) throws Exception {
        externalService.callExternalService();
    }
}
