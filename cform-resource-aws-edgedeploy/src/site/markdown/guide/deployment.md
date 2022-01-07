<!---
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# CloudFormation deployment

You need to upload packaged resource handler (`wrzasqpl-aws-edgedeploy.zip` from
[GitHub Releases](https://github.com/rafalwrzeszcz-wrzasqpl/pl.wrzasq.cform/releases)) to your **S3** bucket.
Afterwards you can just execute following template:

```yaml
Resources:
    LogGroup:
        Type: "AWS::Logs::LogGroup"
        Properties:
            LogGroupName: "/aws/cloudformation/type/WrzasqPl-AWS-EdgeDeploy/"
            RetentionInDays: 14

    ExecutionRole:
        Type: "AWS::IAM::Role"
        Properties:
            AssumeRolePolicyDocument:
                Version: "2012-10-17"
                Statement:
                    -
                        Effect: "Allow"
                        Principal:
                            Service: "resources.cloudformation.amazonaws.com"
                        Action: "sts:AssumeRole"
            Policies:
                -
                    PolicyName: "AllowLoadingPackages"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "s3:GetBucketLocation"
                                    - "s3:GetObject"
                                Effect: "Allow"
                                Resource:
                                    - "*"
                -
                    PolicyName: "AllowManagingEdgeLambdas"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "lambda:CreateFunction"
                                    - "lambda:DeleteFunction"
                                    - "lambda:GetFunction"
                                    - "lambda:GetFunctionConfiguration"
                                    - "lambda:ListTags"
                                    - "lambda:ListVersionsByFunction"
                                    - "lambda:PublishVersion"
                                    - "lambda:TagResource"
                                    - "lambda:UntagResource"
                                    - "lambda:UpdateFunctionCode"
                                    - "lambda:UpdateFunctionConfiguration"
                                Effect: "Allow"
                                Resource:
                                    - !Sub "arn:aws:lambda:us-east-1:${AWS::AccountId}:function:*"
                -
                    PolicyName: "AllowPassingEdgeRole"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "iam:PassRole"
                                Effect: "Allow"
                                Resource:
                                    - "*"

    LoggingRole:
        Type: "AWS::IAM::Role"
        Properties:
            AssumeRolePolicyDocument:
                Version: "2012-10-17"
                Statement:
                    -
                        Effect: "Allow"
                        Principal:
                            Service: "resources.cloudformation.amazonaws.com"
                        Action: "sts:AssumeRole"
            Policies:
                -
                    PolicyName: "AllowSendingMetrics"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Effect: "Allow"
                                Action:
                                    - "cloudwatch:ListMetrics"
                                    - "cloudwatch:PutMetricData"
                                    - "logs:CreateLogGroup"
                                    - "logs:CreateLogStream"
                                    - "logs:DescribeLogGroups"
                                    - "logs:DescribeLogStreams"
                                    - "logs:PutLogEvents"
                                Resource:
                                    - "*"

    EdgeDeployHandler:
        Type: "AWS::CloudFormation::ResourceVersion"
        Properties: 
            ExecutionRoleArn: !GetAtt "ExecutionRole.Arn"
            LoggingConfig: 
                LogGroupName: !Ref "LogGroup"
                LogRoleArn: !GetAtt "LoggingRole.Arn"
            SchemaHandlerPackage: "s3://your-bucket-name/wrzasqpl-aws-edgedeploy.zip"
            TypeName: "WrzasqPl::AWS::EdgeDeploy"

    EdgeDeployVersion:
        Type: "AWS::CloudFormation::ResourceDefaultVersion"
        Properties: 
            TypeVersionArn: !Ref "EdgeDeployHandler"
```

## Serverless repository

The easiest way to distribute such package would be **AWS Serverless Repository**, but unfortunately it doesn't support
`AWS::CloudFormation::ResourceVersion` and `AWS::CloudFormation::ResourceDefaultVersion` resource types.
