package com.example.s3lambdafilemover.lambda;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.example.s3lambdafilemover.service.S3FileMoverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.function.Function;


@Slf4j
public class S3LambdaHandler implements Function<S3Event, String> {

    private static volatile ConfigurableApplicationContext applicationContext;
    private static volatile S3FileMoverService s3FileMoverService;


    @Override
    public String apply(S3Event s3Event) {
        try {
            log.info("Processing S3 event");
            
            if (applicationContext == null) {
                synchronized (S3LambdaHandler.class) {
                    if (applicationContext == null) {
                        log.info("Initializing Spring context for Lambda");
                        applicationContext = new AnnotationConfigApplicationContext(LambdaConfig.class);
                        s3FileMoverService = applicationContext.getBean(S3FileMoverService.class);
                        log.info("Spring context initialized successfully");
                    }
                }
            }

            return s3FileMoverService.processS3Event(s3Event);
        } catch (Exception e) {
            log.error("Error processing S3 event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process S3 event", e);
        }
    }
} 