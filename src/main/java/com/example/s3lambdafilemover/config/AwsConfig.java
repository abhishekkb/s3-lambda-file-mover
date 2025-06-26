package com.example.s3lambdafilemover.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Configuration
public class AwsConfig {

    @Bean
    @ConfigurationProperties(prefix = "aws")
    public AwsProperties awsProperties() {
        return new AwsProperties();
    }

    @Bean
    public S3Client s3Client(AwsProperties awsProperties) {
        return S3Client.builder()
                .region(Region.of(awsProperties.getRegion()))
                .build();
    }

    @Bean
    public LambdaClient lambdaClient(AwsProperties awsProperties) {
        return LambdaClient.builder()
                .region(Region.of(awsProperties.getRegion()))
                .build();
    }

    @Data
    public static class AwsProperties {
        private String region = "us-east-1";
        private S3Properties s3 = new S3Properties();
        private LambdaProperties lambda = new LambdaProperties();

        @Data
        public static class S3Properties {
            private String sourceBucket;
            private String destinationBucket;
            private String sourcePrefix = "";
            private String destinationPrefix = "";
        }

        @Data
        public static class LambdaProperties {
            private String functionName = "s3-file-copier";
            private int timeoutSeconds = 300;
            private int memorySize = 512;
        }
    }
} 