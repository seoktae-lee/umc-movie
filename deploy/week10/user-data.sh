#!/bin/bash
# EC2 첫 부팅 시 실행. 비밀 값은 SSM에서 읽어 root만 읽을 수 있는 환경 파일에 둔다.
set -euo pipefail
exec > >(tee -a /var/log/umc-movie-bootstrap.log) 2>&1
NAME=__NAME__
BUCKET=__BUCKET__

dnf install -y java-21-amazon-corretto-headless mariadb105
TOKEN=$(curl -fsS -X PUT -H 'X-aws-ec2-metadata-token-ttl-seconds: 300' http://169.254.169.254/latest/api/token)
REGION=$(curl -fsS -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/placement/region)
param() { aws ssm get-parameter --region "$REGION" --name "/$NAME/$1" --with-decryption --query Parameter.Value --output text; }
DB_HOST=$(aws rds describe-db-instances --region "$REGION" --db-instance-identifier "$NAME" --query 'DBInstances[0].Endpoint.Address' --output text)
DB_PASSWORD=$(param db-password)

mkdir -p /opt/movie
aws s3 cp "s3://$BUCKET/movie.jar" /opt/movie/movie.jar --region "$REGION"
aws s3 cp "s3://$BUCKET/schema.sql" /opt/movie/schema.sql --region "$REGION"

# 스키마가 없을 때만 적용 (운영 앱은 ddl-auto=validate라 테이블을 만들지 않는다)
if ! MYSQL_PWD="$DB_PASSWORD" mysql -h "$DB_HOST" -u admin -Nse 'SELECT 1 FROM movie_db.member LIMIT 0' >/dev/null 2>&1; then
  MYSQL_PWD="$DB_PASSWORD" mysql -h "$DB_HOST" -u admin movie_db < /opt/movie/schema.sql
fi

umask 077
cat > /etc/movie.env <<ENV
SPRING_PROFILES_ACTIVE=prod
DB_HOST=$DB_HOST
DB_USER=admin
DB_PASSWORD=$DB_PASSWORD
JWT_SECRET=$(param jwt-secret)
JWT_ACCESS_EXPIRATION=3600
FRONTEND_ORIGINS=$(param frontend-origins)
ENV
umask 022

# SSH/세션을 닫아도 계속 실행되고, 재부팅 시 자동 시작되도록 systemd 서비스로 등록한다.
cat > /etc/systemd/system/movie.service <<'UNIT'
[Unit]
Description=UMC movie API (week10)
After=network-online.target
[Service]
User=ec2-user
EnvironmentFile=/etc/movie.env
ExecStart=/usr/bin/java -Xmx512m -jar /opt/movie/movie.jar
StandardOutput=append:/home/ec2-user/movie-app.log
StandardError=append:/home/ec2-user/movie-app.log
Restart=on-failure
[Install]
WantedBy=multi-user.target
UNIT
chmod 600 /etc/movie.env  # systemd(root)가 읽어서 넘긴다. ec2-user는 읽을 수 없다.
systemctl daemon-reload
systemctl enable --now movie
