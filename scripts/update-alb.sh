#!/bin/bash
# update-alb.sh
# Run this script after ECS task restarts to register the new private IP with ALB
# Usage: ./scripts/update-alb.sh

set -e

echo "Getting current ECS task private IP..."

NEW_IP=$(aws ecs describe-tasks \
  --cluster vehicleguard-cluster \
  --tasks $(aws ecs list-tasks \
    --cluster vehicleguard-cluster \
    --service-name vehicleguard-risk-engine \
    --region us-east-2 \
    --query 'taskArns[0]' \
    --output text) \
  --region us-east-2 \
  --query 'tasks[0].containers[0].networkInterfaces[0].privateIpv4Address' \
  --output text)

echo "New private IP: $NEW_IP"

TG_ARN=$(aws elbv2 describe-target-groups \
  --names vehicleguard-risk-tg \
  --region us-east-2 \
  --query 'TargetGroups[0].TargetGroupArn' \
  --output text)

echo "Registering target with ALB..."

aws elbv2 register-targets \
  --target-group-arn $TG_ARN \
  --targets Id=$NEW_IP,Port=8080 \
  --region us-east-2

echo "Done! Target $NEW_IP registered on port 8080."
echo "Check health status in AWS Console: EC2 > Target Groups > vehicleguard-risk-tg > Targets"
