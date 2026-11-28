#!/usr/bin/env bash
# 현재 EC2 공개 주소와 RDS·EC2 상태 (CloudShell)
export AWS_DEFAULT_REGION=ap-southeast-2
NAME=umc-movie-week10
aws ec2 describe-instances --filters Name=tag:Name,Values="$NAME" Name=instance-state-name,Values=pending,running,stopped \
  --query 'Reservations[].Instances[].[InstanceId,State.Name,InstanceType,PublicIpAddress]' --output table
aws rds describe-db-instances --db-instance-identifier "$NAME" \
  --query 'DBInstances[].[DBInstanceIdentifier,DBInstanceStatus,DBInstanceClass,PubliclyAccessible]' --output table
