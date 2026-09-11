package com.training;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.training.mapper")
public class TrainingSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrainingSystemApplication.class, args);
    }
}
