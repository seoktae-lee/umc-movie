#!/usr/bin/env bash
set -euo pipefail
export AWS_DEFAULT_REGION=ap-southeast-2 AWS_PAGER=""
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGISTRY="$ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com"
IMAGE="$REGISTRY/umc-movie-week1:week2-d10bff3"
aws ecr get-login-password | docker login --username AWS --password-stdin "$REGISTRY"
docker tag umc-movie:week2 "$IMAGE"
docker push "$IMAGE"
export IMAGE
python3 - <<'PY'
import base64,json,os
source=open('infra/week2/bootstrap-ec2.sh').read()
source=source.replace('#!/bin/bash\n','#!/bin/bash\nAPP_IMAGE='+os.environ['IMAGE']+'\n',1)
json.dump({'UserData':base64.b64encode(source.encode()).decode()},open('/tmp/umc-week2-template.json','w'))
PY
VERSION=$(aws ec2 create-launch-template-version --launch-template-name umc-movie-week1 --source-version 2 --launch-template-data file:///tmp/umc-week2-template.json --query LaunchTemplateVersion.VersionNumber --output text)
aws autoscaling start-instance-refresh --auto-scaling-group-name umc-movie-week1 --strategy Rolling \
  --desired-configuration "{\"LaunchTemplate\":{\"LaunchTemplateName\":\"umc-movie-week1\",\"Version\":\"$VERSION\"}}" \
  --preferences '{"MinHealthyPercentage":100,"MaxHealthyPercentage":150,"InstanceWarmup":30,"SkipMatching":false}' \
  --query InstanceRefreshId --output text
echo "Launch template version: $VERSION"
