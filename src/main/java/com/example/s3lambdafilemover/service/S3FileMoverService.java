package com.example.s3lambdafilemover.service;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.example.s3lambdafilemover.config.AwsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileMoverService {

    private final S3Client s3Client;
    private final AwsConfig.AwsProperties awsProperties;

    public String processS3Event(S3Event s3Event) {
        try {
            for (S3EventNotification.S3EventNotificationRecord record : s3Event.getRecords()) {
                S3EventNotification.S3Entity s3Entity = record.getS3();
                String sourceBucket = s3Entity.getBucket().getName();
                String sourceKey = s3Entity.getObject().getKey();
                
                log.info("Processing file: s3://{}/{}", sourceBucket, sourceKey);
                
                if (!sourceBucket.equals(awsProperties.getS3().getSourceBucket())) {
                    log.info("Skipping file from bucket {} (not the configured source bucket)", sourceBucket);
                    continue;
                }
                
                if (!sourceKey.startsWith(awsProperties.getS3().getSourcePrefix())) {
                    log.info("Skipping file {} (doesn't match source prefix: {})", 
                              sourceKey, awsProperties.getS3().getSourcePrefix());
                    continue;
                }
                
                String destinationKey = buildDestinationKey(sourceKey);
                copyFileWithTagging(sourceBucket, sourceKey, destinationKey);
                
                log.info("Successfully copied file from s3://{}/{} to s3://{}/{}", 
                          sourceBucket, sourceKey, awsProperties.getS3().getDestinationBucket(), destinationKey);
            }
            
            return "Successfully processed S3 event";
        } catch (Exception e) {
            log.error("Error processing S3 event", e);
            throw new RuntimeException("Failed to process S3 event", e);
        }
    }

    private void copyFileWithTagging(String sourceBucket, String sourceKey, String destinationKey) {
        try {
            CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                    .sourceBucket(sourceBucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(awsProperties.getS3().getDestinationBucket())
                    .destinationKey(destinationKey)
                    .tagging(buildTaggingString())
                    .build();

            log.debug("Copying object from s3://{}/{} to s3://{}/{}", 
                        sourceBucket, sourceKey, awsProperties.getS3().getDestinationBucket(), destinationKey);

            CopyObjectResponse copyResponse = s3Client.copyObject(copyObjectRequest);
            log.debug("Copy response: {}", copyResponse);

            log.info("File copied successfully with tags");

        } catch (S3Exception e) {
            log.error("S3 error while copying file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to copy file in S3", e);
        }
    }
    private String buildDestinationKey(String sourceKey) {
        String destinationPrefix = awsProperties.getS3().getDestinationPrefix();
        if (destinationPrefix == null || destinationPrefix.isEmpty()) {
            return sourceKey;
        }
        
        String keyWithoutSourcePrefix = sourceKey;
        String sourcePrefix = awsProperties.getS3().getSourcePrefix();
        if (sourcePrefix != null && !sourcePrefix.isEmpty() && sourceKey.startsWith(sourcePrefix)) {
            keyWithoutSourcePrefix = sourceKey.substring(sourcePrefix.length());
        }
        
        return destinationPrefix + keyWithoutSourcePrefix;
    }

    private String buildTaggingString() {
        Map<String, String> tags = new HashMap<>();
        tags.put("CopiedBy", "S3LambdaFileCopier");
        tags.put("CopiedAt", Instant.now().toString());
        tags.put("SourceBucket", awsProperties.getS3().getSourceBucket());
        tags.put("DestinationBucket", awsProperties.getS3().getDestinationBucket());
        tags.put("ProcessingType", "Copy");
        tags.put("Environment", "Production");
        
        StringBuilder taggingString = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            if (!first) {
                taggingString.append("&");
            }
            taggingString.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        
        return taggingString.toString();
    }

    public HeadObjectResponse getFileMetadata(String bucket, String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            
            return s3Client.headObject(headObjectRequest);
        } catch (S3Exception e) {
            log.error("Error getting file metadata for s3://{}/{}: {}", bucket, key, e.getMessage());
            throw new RuntimeException("Failed to get file metadata", e);
        }
    }
} 