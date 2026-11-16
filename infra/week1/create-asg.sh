#!/usr/bin/env bash
set -euo pipefail
export AWS_DEFAULT_REGION=ap-southeast-2
NAME=umc-movie-week1
VPC=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)
SUBNETS=$(aws ec2 describe-subnets --filters Name=vpc-id,Values="$VPC" --query 'Subnets[].SubnetId' --output text)
EC2_SG=$(aws ec2 describe-security-groups --filters Name=group-name,Values="$NAME-ec2" --query 'SecurityGroups[0].GroupId' --output text)
ALB_SG=$(aws ec2 create-security-group --vpc-id "$VPC" --group-name "$NAME-alb" --description 'UMC week1 HTTP ALB' --query GroupId --output text)
aws ec2 authorize-security-group-ingress --group-id "$ALB_SG" --protocol tcp --port 80 --cidr 0.0.0.0/0 >/dev/null
aws ec2 authorize-security-group-ingress --group-id "$EC2_SG" --protocol tcp --port 8080 --source-group "$ALB_SG" >/dev/null
read -ra SUBNET_ARRAY <<< "$SUBNETS"
ALB=$(aws elbv2 create-load-balancer --name "$NAME" --subnets "${SUBNET_ARRAY[@]}" --security-groups "$ALB_SG" --scheme internet-facing --type application --query 'LoadBalancers[0].LoadBalancerArn' --output text)
TG=$(aws elbv2 create-target-group --name "$NAME" --protocol HTTP --port 8080 --vpc-id "$VPC" --health-check-path /health --health-check-interval-seconds 15 --healthy-threshold-count 2 --query 'TargetGroups[0].TargetGroupArn' --output text)
aws elbv2 modify-target-group-attributes --target-group-arn "$TG" --attributes Key=deregistration_delay.timeout_seconds,Value=30 >/dev/null
aws elbv2 create-listener --load-balancer-arn "$ALB" --protocol HTTP --port 80 --default-actions Type=forward,TargetGroupArn="$TG" >/dev/null
AMI=$(aws ssm get-parameter --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 --query Parameter.Value --output text)
export AMI EC2_SG
python3 - <<'PY'
import base64,json,os
data={'ImageId':os.environ['AMI'],'InstanceType':'t3.micro','IamInstanceProfile':{'Name':'umc-movie-week1-ec2'},'SecurityGroupIds':[os.environ['EC2_SG']],'UserData':base64.b64encode(open('infra/week1/bootstrap-ec2.sh','rb').read()).decode(),'MetadataOptions':{'HttpTokens':'required'},'CreditSpecification':{'CpuCredits':'standard'},'BlockDeviceMappings':[{'DeviceName':'/dev/xvda','Ebs':{'VolumeSize':12,'VolumeType':'gp3','Encrypted':True,'DeleteOnTermination':True}}]}
json.dump(data,open('/tmp/umc-launch-template.json','w'))
PY
aws ec2 create-launch-template --launch-template-name "$NAME" --launch-template-data file:///tmp/umc-launch-template.json >/dev/null
aws autoscaling create-auto-scaling-group --auto-scaling-group-name "$NAME" --launch-template "LaunchTemplateName=$NAME,Version=1" --min-size 2 --max-size 3 --desired-capacity 2 --vpc-zone-identifier "${SUBNETS//$'\t'/,}" --target-group-arns "$TG" --health-check-type ELB --health-check-grace-period 600
aws elbv2 describe-load-balancers --load-balancer-arns "$ALB" --query 'LoadBalancers[0].DNSName' --output text
