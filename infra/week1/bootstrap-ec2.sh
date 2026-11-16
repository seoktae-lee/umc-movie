#!/bin/bash
set -euo pipefail

exec > >(tee -a /var/log/umc-movie-bootstrap.log) 2>&1

dnf install -y docker awscli
systemctl enable --now docker

IMDS_TOKEN=$(curl -fsS -X PUT -H 'X-aws-ec2-metadata-token-ttl-seconds: 21600' http://169.254.169.254/latest/api/token)
REGION=$(curl -fsS -H "X-aws-ec2-metadata-token: $IMDS_TOKEN" http://169.254.169.254/latest/meta-data/placement/region)
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
DB_HOST=$(aws rds describe-db-instances --region "$REGION" --db-instance-identifier umc-movie-week1 --query 'DBInstances[0].Endpoint.Address' --output text)
DB_PASSWORD=$(aws ssm get-parameter --region "$REGION" --name /umc-movie/week1/db-password --with-decryption --query Parameter.Value --output text)
JWT_SECRET=$(aws ssm get-parameter --region "$REGION" --name /umc-movie/week1/jwt-secret --with-decryption --query Parameter.Value --output text)

for attempt in $(seq 1 60); do
  if MYSQL_PWD="$DB_PASSWORD" docker run --rm -e MYSQL_PWD mysql:8.4 mysqladmin ping -h "$DB_HOST" -u admin --silent; then
    break
  fi
  sleep 10
done

curl -fsSL https://raw.githubusercontent.com/seoktae-lee/umc-movie/infra/week1-prep/docs/sql/01_schema.sql -o /tmp/umc-schema.sql
MYSQL_PWD="$DB_PASSWORD" docker run --rm -e MYSQL_PWD -v /tmp/umc-schema.sql:/schema.sql:ro mysql:8.4 \
  sh -c 'mysql -h "$1" -u admin < /schema.sql' sh "$DB_HOST"

aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"
docker pull "$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/umc-movie-week1:latest"

docker run -d --name umc-movie --restart unless-stopped -p 8080:8080 \
  -e DB_URL="jdbc:mysql://$DB_HOST:3306/umc_movie" \
  -e DB_USER=admin \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e JWT_SECRET="$JWT_SECRET" \
  -e JWT_ACCESS_EXPIRATION=3600 \
  -e JAVA_TOOL_OPTIONS=-Xmx512m \
  "$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/umc-movie-week1:latest"
