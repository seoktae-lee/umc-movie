#!/usr/bin/env bash
set -euo pipefail
export AWS_DEFAULT_REGION=ap-southeast-2 AWS_PAGER=""
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
aws ssm put-parameter --name /umc-movie/week2/gc-prom-url --type String --overwrite --value https://prometheus-prod-49-prod-ap-northeast-0.grafana.net/api/prom/push >/dev/null
aws ssm put-parameter --name /umc-movie/week2/gc-prom-user --type String --overwrite --value 3799586 >/dev/null
cat >/tmp/umc-week2-policy.json <<JSON
{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Action":["ssm:GetParameter"],"Resource":"arn:aws:ssm:ap-southeast-2:$ACCOUNT_ID:parameter/umc-movie/week2/*"}]}
JSON
aws iam put-role-policy --role-name umc-movie-week1-ec2 --policy-name umc-movie-week2-observe --policy-document file:///tmp/umc-week2-policy.json
if ! aws ssm get-parameter --name /umc-movie/week2/mysql-exporter-password >/dev/null 2>&1; then
  aws ssm put-parameter --name /umc-movie/week2/mysql-exporter-password --type SecureString --value "$(openssl rand -hex 16)" >/dev/null
fi
DB_HOST=$(aws rds describe-db-instances --db-instance-identifier umc-movie-week1 --query 'DBInstances[0].Endpoint.Address' --output text)
EXPORTER_PASSWORD=$(aws ssm get-parameter --name /umc-movie/week2/mysql-exporter-password --with-decryption --query Parameter.Value --output text)
aws ssm put-parameter --name /umc-movie/week2/mysql-dsn --type SecureString --overwrite --value "umc_exporter:$EXPORTER_PASSWORD@tcp($DB_HOST:3306)/" >/dev/null
unset EXPORTER_PASSWORD
INSTANCE_ID=$(aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names umc-movie-week1 --query 'AutoScalingGroups[0].Instances[0].InstanceId' --output text)
python3 - <<'PY'
import json
command = '''set -eu
export AWS_DEFAULT_REGION=ap-southeast-2
export DB_HOST=$(aws rds describe-db-instances --db-instance-identifier umc-movie-week1 --query 'DBInstances[0].Endpoint.Address' --output text)
export DB_PASSWORD=$(aws ssm get-parameter --name /umc-movie/week1/db-password --with-decryption --query Parameter.Value --output text)
export EXPORTER_PASSWORD=$(aws ssm get-parameter --name /umc-movie/week2/mysql-exporter-password --with-decryption --query Parameter.Value --output text)
curl -fsSL https://raw.githubusercontent.com/seoktae-lee/umc-movie/infra/week2-observability/infra/week2/setup-mysql-exporter.py -o /tmp/umc-setup-exporter.py
docker run --rm -e DB_HOST -e DB_PASSWORD -e EXPORTER_PASSWORD -v /tmp/umc-setup-exporter.py:/setup.py:ro python:3.12-alpine sh -c 'pip install --quiet pymysql cryptography && python /setup.py'
'''
json.dump({'commands':[command]},open('/tmp/umc-week2-mysql-command.json','w'))
PY
COMMAND_ID=$(aws ssm send-command --instance-ids "$INSTANCE_ID" --document-name AWS-RunShellScript --parameters file:///tmp/umc-week2-mysql-command.json --query Command.CommandId --output text)
echo "Exporter setup command: $COMMAND_ID on $INSTANCE_ID"
aws ssm wait command-executed --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID"
aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" --query '{Status:Status,Output:StandardOutputContent,Error:StandardErrorContent}' --output json
