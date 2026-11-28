#!/usr/bin/env bash
# 실습 종료 후 비용이 나가는 리소스를 모두 지운다. (스냅샷 없이 삭제 — 실습 데이터만 있음)
set -uo pipefail
export AWS_DEFAULT_REGION=ap-southeast-2
NAME=umc-movie-week10
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
IDS=$(aws ec2 describe-instances --filters Name=tag:Name,Values="$NAME" Name=instance-state-name,Values=pending,running,stopped --query 'Reservations[].Instances[].InstanceId' --output text)
[[ -n "$IDS" ]] && aws ec2 terminate-instances --instance-ids $IDS >/dev/null && aws ec2 wait instance-terminated --instance-ids $IDS
aws rds delete-db-instance --db-instance-identifier "$NAME" --skip-final-snapshot --delete-automated-backups >/dev/null && \
  aws rds wait db-instance-deleted --db-instance-identifier "$NAME"
aws rds delete-db-subnet-group --db-subnet-group-name "$NAME"
VPC_ID=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)
for g in db ec2; do
  SG=$(aws ec2 describe-security-groups --filters Name=vpc-id,Values="$VPC_ID" Name=group-name,Values="$NAME-$g" --query 'SecurityGroups[0].GroupId' --output text)
  [[ "$SG" != None ]] && aws ec2 delete-security-group --group-id "$SG"
done
aws s3 rb "s3://$NAME-$ACCOUNT_ID" --force
aws ssm delete-parameters --names "/$NAME/db-password" "/$NAME/jwt-secret" "/$NAME/frontend-origins" >/dev/null
aws iam remove-role-from-instance-profile --instance-profile-name "$NAME-ec2" --role-name "$NAME-ec2"
aws iam delete-instance-profile --instance-profile-name "$NAME-ec2"
aws iam delete-role-policy --role-name "$NAME-ec2" --policy-name "$NAME-app"
aws iam detach-role-policy --role-name "$NAME-ec2" --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
aws iam delete-role --role-name "$NAME-ec2"
echo "정리 완료"
