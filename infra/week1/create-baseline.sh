#!/usr/bin/env bash
set -euo pipefail

REGION=ap-southeast-2
export AWS_DEFAULT_REGION="$REGION"
NAME=umc-movie-week1
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
VPC_ID=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)
mapfile -t SUBNET_IDS < <(aws ec2 describe-subnets --filters Name=vpc-id,Values="$VPC_ID" --query 'Subnets[].SubnetId' --output text | tr '\t' '\n')
if (( ${#SUBNET_IDS[@]} < 2 )); then
  echo 'At least two default subnets are required' >&2
  exit 1
fi

aws ecr describe-repositories --repository-names "$NAME" >/dev/null 2>&1 || \
  aws ecr create-repository --repository-name "$NAME" --image-scanning-configuration scanOnPush=true >/dev/null

EC2_SG=$(aws ec2 describe-security-groups --filters Name=vpc-id,Values="$VPC_ID" Name=group-name,Values="$NAME-ec2" --query 'SecurityGroups[0].GroupId' --output text)
if [[ "$EC2_SG" == None ]]; then
  EC2_SG=$(aws ec2 create-security-group --vpc-id "$VPC_ID" --group-name "$NAME-ec2" --description 'UMC movie week1 EC2' --query GroupId --output text)
  aws ec2 authorize-security-group-ingress --group-id "$EC2_SG" --protocol tcp --port 8080 --cidr 0.0.0.0/0
fi

DB_SG=$(aws ec2 describe-security-groups --filters Name=vpc-id,Values="$VPC_ID" Name=group-name,Values="$NAME-db" --query 'SecurityGroups[0].GroupId' --output text)
if [[ "$DB_SG" == None ]]; then
  DB_SG=$(aws ec2 create-security-group --vpc-id "$VPC_ID" --group-name "$NAME-db" --description 'UMC movie week1 RDS' --query GroupId --output text)
  aws ec2 authorize-security-group-ingress --group-id "$DB_SG" --protocol tcp --port 3306 --source-group "$EC2_SG"
fi

aws rds describe-db-subnet-groups --db-subnet-group-name "$NAME" >/dev/null 2>&1 || \
  aws rds create-db-subnet-group --db-subnet-group-name "$NAME" --db-subnet-group-description 'UMC movie week1' --subnet-ids "${SUBNET_IDS[@]}" >/dev/null

if ! aws ssm get-parameter --name /umc-movie/week1/db-password >/dev/null 2>&1; then
  DB_PASSWORD=$(openssl rand -hex 24)
  aws ssm put-parameter --name /umc-movie/week1/db-password --type SecureString --value "$DB_PASSWORD" >/dev/null
fi
if ! aws ssm get-parameter --name /umc-movie/week1/jwt-secret >/dev/null 2>&1; then
  JWT_SECRET=$(openssl rand -hex 32)
  aws ssm put-parameter --name /umc-movie/week1/jwt-secret --type SecureString --value "$JWT_SECRET" >/dev/null
fi

cat > /tmp/umc-movie-week1-trust.json <<'JSON'
{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"ec2.amazonaws.com"},"Action":"sts:AssumeRole"}]}
JSON
if ! aws iam get-role --role-name "$NAME-ec2" >/dev/null 2>&1; then
  aws iam create-role --role-name "$NAME-ec2" --assume-role-policy-document file:///tmp/umc-movie-week1-trust.json >/dev/null
fi
aws iam attach-role-policy --role-name "$NAME-ec2" --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
cat > /tmp/umc-movie-week1-policy.json <<JSON
{"Version":"2012-10-17","Statement":[
  {"Effect":"Allow","Action":["ssm:GetParameter"],"Resource":"arn:aws:ssm:$REGION:$ACCOUNT_ID:parameter/umc-movie/week1/*"},
  {"Effect":"Allow","Action":["ecr:GetAuthorizationToken"],"Resource":"*"},
  {"Effect":"Allow","Action":["ecr:BatchCheckLayerAvailability","ecr:BatchGetImage","ecr:GetDownloadUrlForLayer"],"Resource":"arn:aws:ecr:$REGION:$ACCOUNT_ID:repository/$NAME"},
  {"Effect":"Allow","Action":["rds:DescribeDBInstances"],"Resource":"*"}
]}
JSON
aws iam put-role-policy --role-name "$NAME-ec2" --policy-name "$NAME-bootstrap" --policy-document file:///tmp/umc-movie-week1-policy.json
if ! aws iam get-instance-profile --instance-profile-name "$NAME-ec2" >/dev/null 2>&1; then
  aws iam create-instance-profile --instance-profile-name "$NAME-ec2" >/dev/null
  aws iam add-role-to-instance-profile --instance-profile-name "$NAME-ec2" --role-name "$NAME-ec2"
fi

if ! aws rds describe-db-instances --db-instance-identifier "$NAME" >/dev/null 2>&1; then
  DB_PASSWORD=$(aws ssm get-parameter --name /umc-movie/week1/db-password --with-decryption --query Parameter.Value --output text)
  aws rds create-db-instance \
    --db-instance-identifier "$NAME" --engine mysql --engine-version 8.4 \
    --db-instance-class db.t4g.micro --allocated-storage 20 --storage-type gp2 \
    --master-username admin --master-user-password "$DB_PASSWORD" \
    --db-subnet-group-name "$NAME" --vpc-security-group-ids "$DB_SG" \
    --no-publicly-accessible --storage-encrypted --backup-retention-period 0 \
    --no-multi-az --tags Key=Project,Value=umc-movie-week1 >/dev/null
  unset DB_PASSWORD
fi

echo "RDS creation started. ECR: $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$NAME"
echo "VPC: $VPC_ID; EC2 security group: $EC2_SG; DB security group: $DB_SG"
