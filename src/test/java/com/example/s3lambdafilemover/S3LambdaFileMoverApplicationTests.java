package com.example.s3lambdafilemover;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;


@SpringBootTest
@TestPropertySource(properties = {
    "aws.s3.source-bucket=test-source-bucket",
    "aws.s3.destination-bucket=test-destination-bucket",
    "aws.region=us-east-1"
})
class S3LambdaFileMoverApplicationTests {

    @Test
    void contextLoads() {
        // context loads
    }
} 