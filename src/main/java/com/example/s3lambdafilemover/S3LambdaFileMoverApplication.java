package com.example.s3lambdafilemover;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class S3LambdaFileMoverApplication {

    public static void main(String[] args) {
        SpringApplication.run(S3LambdaFileMoverApplication.class, args);
        System.out.println("S3 Lambda File Copier Application started successfully!");
        System.out.println("Health check available at: http://localhost:8080/api/s3/health");
    }
} 