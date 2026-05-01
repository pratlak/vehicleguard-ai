# VehicleGuard AI — Runbook
**Operational guide for running and maintaining VehicleGuard AI on AWS**

---

## Live URL
https://dhr91rzgdav9i.cloudfront.net

---

## AWS Resources Quick Reference

| Resource | Value |
|----------|-------|
| Region | us-east-2 (Ohio) |
| ECS Cluster | vehicleguard-cluster |
| RDS Endpoint | vehicleguard-db.c3usku8um6nc.us-east-2.rds.amazonaws.com |
| ALB DNS | vehicleguard-alb-527350867.us-east-2.elb.amazonaws.com |
| CloudFront ID | E2PSR1U07IG0R4 |
| S3 Bucket | vehicleguard-ai-frontend |
| ECR risk-engine | 087242258017.dkr.ecr.us-east-2.amazonaws.com/vehicleguard-risk-engine |
| ECR rates-service | 087242258017.dkr.ecr.us-east-2.amazonaws.com/vehicleguard-rates-service |

---

## 1. Check Service Health

```bash
# Check risk-engine via ALB
curl http://vehicleguard-alb-527350867.us-east-2.elb.amazonaws.com/actuator/health

# Check rates-service directly
curl http://RATES_SERVICE_PUBLIC_IP:8081/actuator/health

# Check via CloudFront
curl https://dhr91rzgdav9i.cloudfront.net/api/v1/risk/assess \
  -X POST -H "Content-Type: application/json" \
  -d '{"driverAge":30,"licenseYears":5,"violationsLast5Yr":0,"accidentsLast5Yr":0,"vehicleMake":"Honda","vehicleModel":"CR-V","vehicleYear":2023,"zipCode":"98004","coverageType":"FULL"}'
```

---

## 2. Update ALB Target (Run after ECS task restarts)

When an ECS task restarts, its private IP changes. Run this script to register the new IP:

```bash
./scripts/update-alb.sh
```

Or manually:

```bash
# Get new private IP
NEW_IP=$(aws ecs describe-tasks \
  --cluster vehicleguard-cluster \
  --tasks $(aws ecs list-tasks --cluster vehicleguard-cluster \
    --service-name vehicleguard-risk-engine --region us-east-2 \
    --query 'taskArns[0]' --output text) \
  --region us-east-2 \
  --query 'tasks[0].containers[0].networkInterfaces[0].privateIpv4Address' \
  --output text)

echo "New IP: $NEW_IP"

# Register with ALB target group
TG_ARN=$(aws elbv2 describe-target-groups \
  --names vehicleguard-risk-tg \
  --region us-east-2 \
  --query 'TargetGroups[0].TargetGroupArn' \
  --output text)

aws elbv2 register-targets \
  --target-group-arn $TG_ARN \
  --targets Id=$NEW_IP,Port=8080 \
  --region us-east-2

echo "Done! Check target health in AWS console."
```

---

## 3. Deploy New Backend Version

```bash
# Login to ECR
aws ecr get-login-password --region us-east-2 | \
  docker login --username AWS --password-stdin \
  087242258017.dkr.ecr.us-east-2.amazonaws.com

# Build and push risk-engine (must use amd64 for Fargate)
docker buildx build --platform linux/amd64 -t vehicleguard-risk-engine ./risk-engine
docker tag vehicleguard-risk-engine:latest \
  087242258017.dkr.ecr.us-east-2.amazonaws.com/vehicleguard-risk-engine:latest
docker push \
  087242258017.dkr.ecr.us-east-2.amazonaws.com/vehicleguard-risk-engine:latest

# Build and push rates-service
docker buildx build --platform linux/amd64 -t vehicleguard-rates-service ./rates-service
docker tag vehicleguard-rates-service:latest \
  087242258017.dkr.ecr.us-east-2.amazonaws.com/vehicleguard-rates-service:latest
docker push \
  087242258017.dkr.ecr.us-east-2.amazonaws.com/vehicleguard-rates-service:latest

# Force redeploy on ECS
aws ecs update-service \
  --cluster vehicleguard-cluster \
  --service vehicleguard-risk-engine \
  --force-new-deployment \
  --region us-east-2 \
  --no-cli-pager

# After deployment, update ALB target
./scripts/update-alb.sh
```

---

## 4. Deploy New Frontend Version

```bash
cd frontend

# Build
npm run build

# Deploy to S3
aws s3 sync dist s3://vehicleguard-ai-frontend --delete

# Invalidate CloudFront cache
aws cloudfront create-invalidation \
  --distribution-id E2PSR1U07IG0R4 \
  --paths "/*" \
  --no-cli-pager
```

---

## 5. Run Locally

```bash
cd vehicleguard-ai

# Set API key
echo "ANTHROPIC_API_KEY=your-key-here" > .env

# Start all services
docker-compose up

# Access at http://localhost:3000
```

---

## 6. View Quotes in Database

```bash
# Connect to RDS
export RDSHOST="vehicleguard-db.c3usku8um6nc.us-east-2.rds.amazonaws.com"
psql "host=$RDSHOST port=5432 dbname=vehicleguard user=vehicleguard sslmode=require"

# View all quotes
SELECT id, driver_age, vehicle_make, vehicle_model, 
       risk_tier, monthly_premium_usd, created_at 
FROM quotes.quote_requests 
ORDER BY created_at DESC;

# Count quotes by risk tier
SELECT risk_tier, COUNT(*) 
FROM quotes.quote_requests 
GROUP BY risk_tier;
```

---

## 7. Rotate Anthropic API Key

1. Go to https://console.anthropic.com/api_keys
2. Delete old key, create new one
3. Update ECS task definition:
   - ECS → Task Definitions → vehicleguard-risk-engine → Create new revision
   - Update ANTHROPIC_API_KEY environment variable
4. Redeploy: `aws ecs update-service --cluster vehicleguard-cluster --service vehicleguard-risk-engine --force-new-deployment --region us-east-2 --no-cli-pager`
5. Update local `.env` file
6. Run `./scripts/update-alb.sh` after deployment

---

## 8. Troubleshooting

| Issue | Check | Fix |
|-------|-------|-----|
| 502 Bad Gateway | ALB target health | Run `./scripts/update-alb.sh` |
| Network error on frontend | CloudFront cache | Invalidate CloudFront cache |
| Chatbot not responding | Anthropic API key | Rotate key, update ECS task definition |
| DB connection failed | RDS security group | Check inbound rules allow port 5432 |
| ECS task not starting | ECR image platform | Rebuild with `--platform linux/amd64` |
