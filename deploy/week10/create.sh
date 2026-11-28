#!/usr/bin/env bash
# 10주차: EC2 + RDS 배포 (AWS CloudShell에서 저장소 루트 기준으로 실행)
#   bash deploy/week10/create.sh
# 비밀 값(DB 비밀번호·JWT 키)은 SSM SecureString에만 저장하고 화면·파일에 출력하지 않는다.
set -euo pipefail

REGION=ap-southeast-2
export AWS_DEFAULT_REGION="$REGION"
NAME=umc-movie-week10
FRONTEND_ORIGINS="${FRONTEND_ORIGINS:-http://localhost:5173}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
BUCKET="$NAME-$ACCOUNT_ID"
VPC_ID=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)
mapfile -t SUBNET_IDS < <(aws ec2 describe-subnets --filters Name=vpc-id,Values="$VPC_ID" --query 'Subnets[].SubnetId' --output text | tr '\t' '\n')

# 1) 보안 그룹: 앱 8080은 외부 공개, RDS 3306은 EC2 보안 그룹에서만
EC2_SG=$(aws ec2 describe-security-groups --filters Name=vpc-id,Values="$VPC_ID" Name=group-name,Values="$NAME-ec2" --query 'SecurityGroups[0].GroupId' --output text)
if [[ "$EC2_SG" == None ]]; then
  EC2_SG=$(aws ec2 create-security-group --vpc-id "$VPC_ID" --group-name "$NAME-ec2" --description 'UMC week10 app' --query GroupId --output text)
  aws ec2 authorize-security-group-ingress --group-id "$EC2_SG" --protocol tcp --port 8080 --cidr 0.0.0.0/0 >/dev/null
fi
DB_SG=$(aws ec2 describe-security-groups --filters Name=vpc-id,Values="$VPC_ID" Name=group-name,Values="$NAME-db" --query 'SecurityGroups[0].GroupId' --output text)
if [[ "$DB_SG" == None ]]; then
  DB_SG=$(aws ec2 create-security-group --vpc-id "$VPC_ID" --group-name "$NAME-db" --description 'UMC week10 RDS' --query GroupId --output text)
  aws ec2 authorize-security-group-ingress --group-id "$DB_SG" --protocol tcp --port 3306 --source-group "$EC2_SG" >/dev/null
fi

# 2) 비밀 값 (처음 한 번만 생성)
aws ssm get-parameter --name "/$NAME/db-password" >/dev/null 2>&1 || \
  aws ssm put-parameter --name "/$NAME/db-password" --type SecureString --value "$(openssl rand -hex 16)" >/dev/null
aws ssm get-parameter --name "/$NAME/jwt-secret" >/dev/null 2>&1 || \
  aws ssm put-parameter --name "/$NAME/jwt-secret" --type SecureString --value "$(openssl rand -hex 32)" >/dev/null
aws ssm put-parameter --name "/$NAME/frontend-origins" --type String --value "$FRONTEND_ORIGINS" --overwrite >/dev/null

# 3) RDS MySQL (퍼블릭 액세스 끔, DB 이름 movie_db)
aws rds describe-db-subnet-groups --db-subnet-group-name "$NAME" >/dev/null 2>&1 || \
  aws rds create-db-subnet-group --db-subnet-group-name "$NAME" --db-subnet-group-description 'UMC week10' --subnet-ids "${SUBNET_IDS[@]}" >/dev/null
if ! aws rds describe-db-instances --db-instance-identifier "$NAME" >/dev/null 2>&1; then
  aws rds create-db-instance \
    --db-instance-identifier "$NAME" --engine mysql --engine-version 8.4 \
    --db-instance-class db.t4g.micro --allocated-storage 20 --storage-type gp3 \
    --db-name movie_db --master-username admin \
    --master-user-password "$(aws ssm get-parameter --name "/$NAME/db-password" --with-decryption --query Parameter.Value --output text)" \
    --db-subnet-group-name "$NAME" --vpc-security-group-ids "$DB_SG" \
    --no-publicly-accessible --storage-encrypted --backup-retention-period 0 --no-multi-az \
    --tags Key=Project,Value="$NAME" >/dev/null
fi

# 4) 빌드 산출물(JAR)과 스키마를 비공개 S3 버킷에 올린다.
aws s3api head-bucket --bucket "$BUCKET" 2>/dev/null || \
  aws s3api create-bucket --bucket "$BUCKET" --create-bucket-configuration LocationConstraint="$REGION" >/dev/null
aws s3api put-public-access-block --bucket "$BUCKET" --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
java -version 2>&1 | grep -q "\"21" || sudo dnf install -y -q java-21-amazon-corretto-devel
JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto ./gradlew clean bootJar -q
aws s3 cp build/libs/movie-0.0.1-SNAPSHOT.jar "s3://$BUCKET/movie.jar"
aws s3 cp docs/sql/10_prod_schema.sql "s3://$BUCKET/schema.sql"

# 5) EC2 역할: SSM 접속(SSH 대신), 비밀 값 조회, JAR 다운로드, RDS 주소 조회
cat > /tmp/$NAME-trust.json <<'JSON'
{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"ec2.amazonaws.com"},"Action":"sts:AssumeRole"}]}
JSON
aws iam get-role --role-name "$NAME-ec2" >/dev/null 2>&1 || \
  aws iam create-role --role-name "$NAME-ec2" --assume-role-policy-document file:///tmp/$NAME-trust.json >/dev/null
aws iam attach-role-policy --role-name "$NAME-ec2" --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
cat > /tmp/$NAME-policy.json <<JSON
{"Version":"2012-10-17","Statement":[
  {"Effect":"Allow","Action":["ssm:GetParameter"],"Resource":"arn:aws:ssm:$REGION:$ACCOUNT_ID:parameter/$NAME/*"},
  {"Effect":"Allow","Action":["s3:GetObject"],"Resource":"arn:aws:s3:::$BUCKET/*"},
  {"Effect":"Allow","Action":["rds:DescribeDBInstances"],"Resource":"*"}
]}
JSON
aws iam put-role-policy --role-name "$NAME-ec2" --policy-name "$NAME-app" --policy-document file:///tmp/$NAME-policy.json
if ! aws iam get-instance-profile --instance-profile-name "$NAME-ec2" >/dev/null 2>&1; then
  aws iam create-instance-profile --instance-profile-name "$NAME-ec2" >/dev/null
  aws iam add-role-to-instance-profile --instance-profile-name "$NAME-ec2" --role-name "$NAME-ec2"
  sleep 15 # 인스턴스 프로필 전파 대기
fi

echo "RDS 생성 대기 중... (5~10분)"
aws rds wait db-instance-available --db-instance-identifier "$NAME"

# 6) EC2 (Amazon Linux 2023, t3.micro). user-data가 JDK 설치 → 스키마 적용 → systemd로 앱 실행
AMI=$(aws ssm get-parameter --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 --query Parameter.Value --output text)
EXISTING=$(aws ec2 describe-instances --filters Name=tag:Name,Values="$NAME" Name=instance-state-name,Values=pending,running,stopped --query 'Reservations[].Instances[].InstanceId' --output text)
if [[ -z "$EXISTING" ]]; then
  sed "s/__BUCKET__/$BUCKET/; s/__NAME__/$NAME/" deploy/week10/user-data.sh > /tmp/$NAME-user-data.sh
  aws ec2 run-instances --image-id "$AMI" --instance-type t3.micro \
    --iam-instance-profile Name="$NAME-ec2" --security-group-ids "$EC2_SG" --subnet-id "${SUBNET_IDS[0]}" \
    --associate-public-ip-address --metadata-options HttpTokens=required \
    --block-device-mappings '[{"DeviceName":"/dev/xvda","Ebs":{"VolumeSize":10,"VolumeType":"gp3","Encrypted":true,"DeleteOnTermination":true}}]' \
    --credit-specification CpuCredits=standard --user-data file:///tmp/$NAME-user-data.sh \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$NAME},{Key=Project,Value=$NAME}]" \
    --query 'Instances[0].InstanceId' --output text
fi
echo "완료. 공개 주소는 deploy/week10/status.sh로 확인"
