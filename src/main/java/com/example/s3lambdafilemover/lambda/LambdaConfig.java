package com.example.s3lambdafilemover.lambda;

import com.example.s3lambdafilemover.config.AwsConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = {
    "com.example.s3lambdafilemover.config",
    "com.example.s3lambdafilemover.service"
})
@Import(AwsConfig.class)
public class LambdaConfig {
} 