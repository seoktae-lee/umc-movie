#!/usr/bin/env bash
set -euo pipefail
export AWS_DEFAULT_REGION=ap-southeast-2
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGISTRY="$ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com"
docker image inspect umc-movie:week1 >/dev/null 2>&1 || docker build -t umc-movie:week1 .
aws ecr get-login-password | docker login --username AWS --password-stdin "$REGISTRY"
docker tag umc-movie:week1 "$REGISTRY/umc-movie-week1:latest"
docker push "$REGISTRY/umc-movie-week1:latest"
