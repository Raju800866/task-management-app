#!/bin/bash
# One-time setup: CloudFront in front of S3 (frontend) and EC2 (API).
# Run from your Mac:  bash deploy/aws/setup-cloudfront.sh
set -euo pipefail

REGION=ap-southeast-2
BUCKET=taskapp-frontend-324673386287
INSTANCE_ID=i-06149acf891315c32

# AWS-managed policy IDs (the same in every account)
CACHING_OPTIMIZED=658327ea-f89d-4fab-a63d-7e88639e58f6   # cache static files
CACHING_DISABLED=4135ea2d-6df8-44a3-9df3-4b5a84be39ad    # never cache API calls
ALL_VIEWER_EXCEPT_HOST=b689b0a8-53d0-40ab-baf2-68738e2966ac  # forward headers (incl. Authorization), query strings, cookies

echo "1/4  Looking up the EC2 server's public DNS name..."
EC2_DNS=$(aws ec2 describe-instances --region $REGION --instance-ids $INSTANCE_ID \
  --query 'Reservations[0].Instances[0].PublicDnsName' --output text)
echo "     $EC2_DNS"

echo "2/4  Creating Origin Access Control (lets only CloudFront read the private bucket)..."
OAC_ID=$(aws cloudfront list-origin-access-controls \
  --query "OriginAccessControlList.Items[?Name=='taskapp-oac'].Id | [0]" --output text)
if [ "$OAC_ID" = "None" ] || [ -z "$OAC_ID" ]; then
  OAC_ID=$(aws cloudfront create-origin-access-control \
    --origin-access-control-config Name=taskapp-oac,Description="TaskFlow S3 access",SigningProtocol=sigv4,SigningBehavior=always,OriginAccessControlOriginType=s3 \
    --query 'OriginAccessControl.Id' --output text)
fi
echo "     $OAC_ID"

echo "3/4  Creating the CloudFront distribution..."
CONFIG=$(mktemp)
cat > "$CONFIG" <<JSON
{
  "CallerReference": "taskapp-$(date +%s)",
  "Comment": "TaskFlow: React frontend (S3) + API (EC2)",
  "Enabled": true,
  "DefaultRootObject": "index.html",
  "HttpVersion": "http2and3",
  "PriceClass": "PriceClass_All",
  "ViewerCertificate": { "CloudFrontDefaultCertificate": true },
  "Origins": {
    "Quantity": 2,
    "Items": [
      {
        "Id": "s3-frontend",
        "DomainName": "$BUCKET.s3.$REGION.amazonaws.com",
        "OriginAccessControlId": "$OAC_ID",
        "S3OriginConfig": { "OriginAccessIdentity": "" }
      },
      {
        "Id": "ec2-api",
        "DomainName": "$EC2_DNS",
        "CustomOriginConfig": {
          "HTTPPort": 80,
          "HTTPSPort": 443,
          "OriginProtocolPolicy": "http-only",
          "OriginReadTimeout": 30,
          "OriginKeepaliveTimeout": 5,
          "OriginSslProtocols": { "Quantity": 1, "Items": ["TLSv1.2"] }
        }
      }
    ]
  },
  "DefaultCacheBehavior": {
    "TargetOriginId": "s3-frontend",
    "ViewerProtocolPolicy": "redirect-to-https",
    "CachePolicyId": "$CACHING_OPTIMIZED",
    "Compress": true,
    "AllowedMethods": {
      "Quantity": 2, "Items": ["GET", "HEAD"],
      "CachedMethods": { "Quantity": 2, "Items": ["GET", "HEAD"] }
    }
  },
  "CacheBehaviors": {
    "Quantity": 1,
    "Items": [
      {
        "PathPattern": "/api/*",
        "TargetOriginId": "ec2-api",
        "ViewerProtocolPolicy": "redirect-to-https",
        "CachePolicyId": "$CACHING_DISABLED",
        "OriginRequestPolicyId": "$ALL_VIEWER_EXCEPT_HOST",
        "Compress": true,
        "AllowedMethods": {
          "Quantity": 7, "Items": ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"],
          "CachedMethods": { "Quantity": 2, "Items": ["GET", "HEAD"] }
        }
      }
    ]
  },
  "CustomErrorResponses": {
    "Quantity": 1,
    "Items": [
      { "ErrorCode": 403, "ResponsePagePath": "/index.html", "ResponseCode": "200", "ErrorCachingMinTTL": 0 }
    ]
  }
}
JSON

read -r DIST_ID DIST_ARN DIST_DOMAIN <<< "$(aws cloudfront create-distribution \
  --distribution-config "file://$CONFIG" \
  --query 'Distribution.[Id,ARN,DomainName]' --output text)"
rm -f "$CONFIG"
echo "     Id:     $DIST_ID"
echo "     Domain: $DIST_DOMAIN"

echo "4/4  Allowing this distribution (and only it) to read the bucket..."
POLICY=$(mktemp)
cat > "$POLICY" <<JSON
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowCloudFrontRead",
      "Effect": "Allow",
      "Principal": { "Service": "cloudfront.amazonaws.com" },
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::$BUCKET/*",
      "Condition": { "StringEquals": { "AWS:SourceArn": "$DIST_ARN" } }
    }
  ]
}
JSON
aws s3api put-bucket-policy --bucket $BUCKET --policy "file://$POLICY"
rm -f "$POLICY"

echo ""
echo "Done. Save these:"
echo "  Distribution ID: $DIST_ID"
echo "  Live URL:        https://$DIST_DOMAIN"
echo ""
echo "CloudFront takes ~5-10 minutes to deploy worldwide. Check with:"
echo "  aws cloudfront get-distribution --id $DIST_ID --query 'Distribution.Status' --output text"
