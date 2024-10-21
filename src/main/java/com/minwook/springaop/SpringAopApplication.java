package com.minwook.springaop;

import com.minwook.springaop.aop.LoggingAspect;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.minwook.springaop.aop.service.AopService;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}) // JDBC 자동 설정 제외
public class SpringAopApplication {

    public static void main(String[] args)  {
        SpringApplication.run(SpringAopApplication.class, args); // Spring 애플리케이션 실행
    }

    @Bean
    CommandLineRunner run(AopService aopService) {
        return args -> {
            try {
                aopService.executeTest();  // AOP가 적용된 메서드 호출
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
    }

}
