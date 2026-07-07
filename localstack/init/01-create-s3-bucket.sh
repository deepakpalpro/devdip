#!/bin/bash
# Creates the S3 bucket used by the s3-archive downstream connector seam (US-8.1).
set -euo pipefail

BUCKET="${S3_BUCKET:-banking-forms-submissions}"
awslocal s3 mb "s3://${BUCKET}" 2>/dev/null || true
echo "LocalStack S3 buckets:"
awslocal s3 ls
