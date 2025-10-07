#!/bin/bash
set -e

AWS_ACCOUNT_ID="058264553893"
AWS_REGION="ap-southeast-1"
SERVICES=("auth-service" "patient-service" "billing-service" "analytics-service" "api-gateway")

# Log in to ECR
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

for SERVICE in "${SERVICES[@]}"
do
  echo "--- Building and pushing $SERVICE ---"
  docker build -t $SERVICE ./$SERVICE
  docker tag $SERVICE:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$SERVICE:latest
  docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$SERVICE:latest
done

echo "--- All images pushed to ECR successfully! ---"