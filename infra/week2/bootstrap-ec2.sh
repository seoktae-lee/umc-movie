#!/bin/bash
set -euo pipefail

exec > >(tee -a /var/log/umc-movie-bootstrap.log) 2>&1

dnf install -y docker
systemctl enable --now docker

IMDS_TOKEN=$(curl -fsS -X PUT -H 'X-aws-ec2-metadata-token-ttl-seconds: 21600' http://169.254.169.254/latest/api/token)
REGION=$(curl -fsS -H "X-aws-ec2-metadata-token: $IMDS_TOKEN" http://169.254.169.254/latest/meta-data/placement/region)
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
DB_HOST=$(aws rds describe-db-instances --region "$REGION" --db-instance-identifier umc-movie-week1 --query 'DBInstances[0].Endpoint.Address' --output text)
DB_PASSWORD=$(aws ssm get-parameter --region "$REGION" --name /umc-movie/week1/db-password --with-decryption --query Parameter.Value --output text)
JWT_SECRET=$(aws ssm get-parameter --region "$REGION" --name /umc-movie/week1/jwt-secret --with-decryption --query Parameter.Value --output text)

if [[ "${INITIALIZE_SCHEMA:-false}" == true ]]; then
for attempt in $(seq 1 60); do
  if MYSQL_PWD="$DB_PASSWORD" docker run --rm -e MYSQL_PWD mysql:8.4 mysqladmin ping -h "$DB_HOST" -u admin --silent; then
    break
  fi
  sleep 10
done

curl -fsSL https://raw.githubusercontent.com/seoktae-lee/umc-movie/infra/week1-prep/docs/sql/01_schema.sql -o /tmp/umc-schema.sql
MYSQL_PWD="$DB_PASSWORD" docker run --rm -e MYSQL_PWD -v /tmp/umc-schema.sql:/schema.sql:ro mysql:8.4 \
  sh -c 'if ! mysql -h "$1" -u admin -Nse "SELECT member_id FROM umc_movie.member LIMIT 0" >/dev/null 2>&1; then mysql -h "$1" -u admin < /schema.sql; fi' sh "$DB_HOST"

fi

aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"
APP_IMAGE="${APP_IMAGE:?Set an immutable ECR image URI for week2}"
docker pull "$APP_IMAGE"

docker run -d --name umc-movie --restart unless-stopped -p 8080:8080 -p 127.0.0.1:9091:9091 \
  -e DB_URL="jdbc:mysql://$DB_HOST:3306/umc_movie" \
  -e DB_USER=admin \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e JWT_SECRET="$JWT_SECRET" \
  -e JWT_ACCESS_EXPIRATION=3600 \
  -e JAVA_TOOL_OPTIONS=-Xmx512m \
  "$APP_IMAGE"

# Grafana Fleet Management configuration is delivered remotely.
p() { aws ssm get-parameter --region "$REGION" --name "/umc-movie/week2/$1" --with-decryption --query Parameter.Value --output text; }
export GC_FLEET_URL=$(p gc-fleet-url)
export GC_FLEET_USER=$(p gc-fleet-user)
export GC_PROM_URL=$(p gc-prom-url)
export GC_PROM_USER=$(p gc-prom-user)
export GC_TOKEN=$(p gc-token)
export GCLOUD_RW_API_KEY="$GC_TOKEN"
export MYSQL_DSN=$(p mysql-dsn)
export APP_METRICS_ADDR=127.0.0.1:9091
export APP_METRICS_PATH=/actuator/prometheus
mkdir -p /opt/alloy /var/lib/alloy
cat > /opt/alloy/config.alloy <<'ALLOY'
logging { level = "info" }
remotecfg {
  url = sys.env("GC_FLEET_URL")
  basic_auth {
    username = sys.env("GC_FLEET_USER")
    password = sys.env("GC_TOKEN")
  }
  id = constants.hostname
  poll_frequency = "5m"
}
ALLOY

docker run -d --name alloy --restart unless-stopped --network host --pid host \
  -v /:/rootfs:ro -v /var/run/docker.sock:/var/run/docker.sock:ro \
  -v /opt/alloy/config.alloy:/etc/alloy/config.alloy:ro \
  -v /var/lib/alloy:/var/lib/alloy \
  -e GC_FLEET_URL -e GC_FLEET_USER -e GC_PROM_URL -e GC_PROM_USER \
  -e GC_TOKEN -e GCLOUD_RW_API_KEY -e MYSQL_DSN \
  -e APP_METRICS_ADDR -e APP_METRICS_PATH \
  grafana/alloy:v1.20.1 run --storage.path=/var/lib/alloy /etc/alloy/config.alloy
