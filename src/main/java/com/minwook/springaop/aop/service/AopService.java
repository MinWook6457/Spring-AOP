package com.minwook.springaop.aop.service;

import com.minwook.springaop.annotation.AnnotationTest;
import org.springframework.stereotype.Service;

@Service
public class AopService {
    @AnnotationTest
    public void executeTest() throws InterruptedException {
        System.out.println("Executing test method...");
        Thread.sleep(1000);  // 테스트를 위해 일부러 1초 지연
    }
}
