#!/bin/bash

# S3 Lambda File Copier - Deployment Script
# This script builds and deploys the Lambda function to AWS

set -e

# Configuration
STACK_NAME="s3-lambda-file-copier"
REGION="us-east-1"
SOURCE_BUCKET="your-source-bucket-name"
DESTINATION_BUCKET="your-destination-bucket-name"
SOURCE_PREFIX=""
DESTINATION_PREFIX=""
LAMBDA_FUNCTION_NAME="s3-file-copier"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if AWS CLI is installed
check_aws_cli() {
    if ! command -v aws &> /dev/null; then
        print_error "AWS CLI is not installed. Please install it first."
        exit 1
    fi
}

# Check if Maven is installed
check_maven() {
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install it first."
        exit 1
    fi
}

# Build the project
build_project() {
    print_status "Building the project..."
    mvn clean package -DskipTests
    print_status "Build completed successfully!"
}

# Deploy using CloudFormation
deploy_cloudformation() {
    print_status "Deploying CloudFormation stack..."
    
    # Check if stack exists
    if aws cloudformation describe-stacks --stack-name $STACK_NAME --region $REGION &> /dev/null; then
        print_warning "Stack $STACK_NAME already exists. Updating..."
        aws cloudformation update-stack \
            --stack-name $STACK_NAME \
            --template-body file://cloudformation-template.yml \
            --parameters \
                ParameterKey=SourceBucketName,ParameterValue=$SOURCE_BUCKET \
                ParameterKey=DestinationBucketName,ParameterValue=$DESTINATION_BUCKET \
                ParameterKey=SourcePrefix,ParameterValue=$SOURCE_PREFIX \
                ParameterKey=DestinationPrefix,ParameterValue=$DESTINATION_PREFIX \
                ParameterKey=LambdaFunctionName,ParameterValue=$LAMBDA_FUNCTION_NAME \
            --capabilities CAPABILITY_NAMED_IAM \
            --region $REGION
        
        print_status "Waiting for stack update to complete..."
        aws cloudformation wait stack-update-complete --stack-name $STACK_NAME --region $REGION
    else
        print_status "Creating new stack $STACK_NAME..."
        aws cloudformation create-stack \
            --stack-name $STACK_NAME \
            --template-body file://cloudformation-template.yml \
            --parameters \
                ParameterKey=SourceBucketName,ParameterValue=$SOURCE_BUCKET \
                ParameterKey=DestinationBucketName,ParameterValue=$DESTINATION_BUCKET \
                ParameterKey=SourcePrefix,ParameterValue=$SOURCE_PREFIX \
                ParameterKey=DestinationPrefix,ParameterValue=$DESTINATION_PREFIX \
                ParameterKey=LambdaFunctionName,ParameterValue=$LAMBDA_FUNCTION_NAME \
            --capabilities CAPABILITY_NAMED_IAM \
            --region $REGION
        
        print_status "Waiting for stack creation to complete..."
        aws cloudformation wait stack-create-complete --stack-name $STACK_NAME --region $REGION
    fi
    
    print_status "CloudFormation deployment completed!"
}

# Update Lambda function code
update_lambda_code() {
    print_status "Updating Lambda function code..."
    
    # Get the Lambda function ARN
    LAMBDA_ARN=$(aws cloudformation describe-stacks \
        --stack-name $STACK_NAME \
        --region $REGION \
        --query 'Stacks[0].Outputs[?OutputKey==`LambdaFunctionArn`].OutputValue' \
        --output text)
    
    # Update the function code
    aws lambda update-function-code \
        --function-name $LAMBDA_FUNCTION_NAME \
        --zip-file fileb://target/s3-lambda-file-mover-1.0.0.jar \
        --region $REGION
    
    print_status "Lambda function code updated successfully!"
}

# Test the deployment
test_deployment() {
    print_status "Testing deployment..."
    
    # Get stack outputs
    SOURCE_BUCKET_OUTPUT=$(aws cloudformation describe-stacks \
        --stack-name $STACK_NAME \
        --region $REGION \
        --query 'Stacks[0].Outputs[?OutputKey==`SourceBucketName`].OutputValue' \
        --output text)
    
    DESTINATION_BUCKET_OUTPUT=$(aws cloudformation describe-stacks \
        --stack-name $STACK_NAME \
        --region $REGION \
        --query 'Stacks[0].Outputs[?OutputKey==`DestinationBucketName`].OutputValue' \
        --output text)
    
    print_status "Deployment Summary:"
    echo "  Stack Name: $STACK_NAME"
    echo "  Source Bucket: $SOURCE_BUCKET_OUTPUT"
    echo "  Destination Bucket: $DESTINATION_BUCKET_OUTPUT"
    echo "  Lambda Function: $LAMBDA_FUNCTION_NAME"
    echo "  Region: $REGION"
    
    print_status "Deployment completed successfully!"
}

# Main deployment function
main() {
    print_status "Starting S3 Lambda File Mover deployment..."
    
    # Check prerequisites
    check_aws_cli
    check_maven
    
    # Build and deploy
    build_project
    deploy_cloudformation
    update_lambda_code
    test_deployment
    
    print_status "Deployment completed! 🎉"
    print_status "You can now upload files to the source bucket to test the Lambda function."
    print_status "Files will be copied to the destination bucket with tags."
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --source-bucket)
            SOURCE_BUCKET="$2"
            shift 2
            ;;
        --destination-bucket)
            DESTINATION_BUCKET="$2"
            shift 2
            ;;
        --source-prefix)
            SOURCE_PREFIX="$2"
            shift 2
            ;;
        --destination-prefix)
            DESTINATION_PREFIX="$2"
            shift 2
            ;;
        --function-name)
            LAMBDA_FUNCTION_NAME="$2"
            shift 2
            ;;
        --region)
            REGION="$2"
            shift 2
            ;;
        --stack-name)
            STACK_NAME="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  --source-bucket BUCKET      Source S3 bucket name"
            echo "  --destination-bucket BUCKET Destination S3 bucket name"
            echo "  --source-prefix PREFIX      Source bucket prefix filter"
            echo "  --destination-prefix PREFIX Destination bucket prefix"
            echo "  --function-name NAME        Lambda function name"
            echo "  --region REGION             AWS region"
            echo "  --stack-name NAME           CloudFormation stack name"
            echo "  --help                      Show this help message"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Run main function
main 