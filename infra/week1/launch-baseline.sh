#!/usr/bin/env bash
set -euo pipefail
export AWS_DEFAULT_REGION=ap-southeast-2
NAME=umc-movie-week1
aws rds wait db-instance-available --db-instance-identifier "$NAME"
AMI=$(aws ssm get-parameter --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 --query Parameter.Value --output text)
SG=$(aws ec2 describe-security-groups --filters Name=group-name,Values="$NAME-ec2" --query 'SecurityGroups[0].GroupId' --output text)
SUBNET=$(aws ec2 describe-subnets --filters Name=default-for-az,Values=true --query 'Subnets[0].SubnetId' --output text)
{ head -n 1 infra/week1/bootstrap-ec2.sh; echo 'INITIALIZE_SCHEMA=true'; tail -n +2 infra/week1/bootstrap-ec2.sh; } > /tmp/umc-baseline-user-data.sh
aws ec2 run-instances --image-id "$AMI" --instance-type t3.micro \
  --iam-instance-profile Name="$NAME-ec2" --security-group-ids "$SG" --subnet-id "$SUBNET" \
  --associate-public-ip-address --metadata-options HttpTokens=required \
  --block-device-mappings '[{"DeviceName":"/dev/xvda","Ebs":{"VolumeSize":12,"VolumeType":"gp3","Encrypted":true,"DeleteOnTermination":true}}]' \
  --credit-specification CpuCredits=standard \
  --user-data file:///tmp/umc-baseline-user-data.sh \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=umc-movie-week1-baseline},{Key=Project,Value=umc-movie-week1}]' \
  --query 'Instances[0].InstanceId' --output text
