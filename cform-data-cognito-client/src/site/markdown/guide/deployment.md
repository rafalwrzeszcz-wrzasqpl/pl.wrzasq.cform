<!---
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# CloudFormation deployment

You need to upload packaged resource handler (`wrzasqpl-cognito-clientdata.zip` from
[GitHub Releases](https://github.com/rafalwrzeszcz-wrzasqpl/pl.wrzasq.cform/releases)) to your **S3** bucket.
Afterwards you can just execute following template:

```yaml
Resources:
    LogGroup:
        Type: "AWS::Logs::LogGroup"
        Properties:
            LogGroupName: "/aws/cloudformation/type/WrzasqPl-Cognito-ClientData/"
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
                    PolicyName: "ResourceTypePolicy"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Effect: "Allow"
                                Action:
                                    - "cognito-idp:DescribeUserPoolClient"
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

    CognitoClientDataHandler:
        Type: "AWS::CloudFormation::ResourceVersion"
        Properties: 
            ExecutionRoleArn: !GetAtt "ExecutionRole.Arn"
            LoggingConfig: 
                LogGroupName: !Ref "LogGroup"
                LogRoleArn: !GetAtt "LoggingRole.Arn"
            SchemaHandlerPackage: "s3://your-bucket-name/wrzasqpl-cognito-clientdata.zip"
            TypeName: "WrzasqPl::Cognito::ClientData"

    CognitoClientDataVersion:
        Type: "AWS::CloudFormation::ResourceDefaultVersion"
        Properties: 
            TypeVersionArn: !Ref "CognitoClientDataHandler"
```

## Serverless repository

The easiest way to distribute such package would be **AWS Serverless Repository**, but unfortunately it doesn't support
`AWS::CloudFormation::ResourceVersion` and `AWS::CloudFormation::ResourceDefaultVersion` resource types.
