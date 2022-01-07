<!---
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Serverless repository

Macro is available in AWS Serverless Repository as [`wrzasqpl-cform-cloudfront-invalidation` application](https://eu-central-1.console.aws.amazon.com/lambda/home?region=eu-central-1#/create/app?applicationId=arn:aws:serverlessrepo:eu-central-1:117504620086:applications/wrzasqpl-cform-cloudfront-invalidation).

## CloudFormation deployment

You can deploy it using **CloudFormation** by referring to repository:

```yaml
WrzasqPlCformCloudFrontInvalidation:
    Type: "AWS::Serverless::Application"
    Properties:
        Location:
            ApplicationId: "arn:aws:serverlessrepo:eu-central-1:117504620086:applications/wrzasqpl-cform-cloudfront-invalidation"
            SemanticVersion: "1.1.7"
```

# CloudFormation manual deployment

Alternatively, if you need full control over artifacts, you may handle the process entirely on your side - you need to
upload packaged macro handler (`cform-cloudfront-invalidation.zip` from
[GitHub Releases](https://github.com/rafalwrzeszcz-wrzasqpl/pl.wrzasq.cform/releases)) to your **S3** bucket.
Afterwards you can just execute following template:

```yaml
Resources:
    LogGroup:
        Type: "AWS::Logs::LogGroup"
        Properties:
            LogGroupName: !Sub "/aws/lambda/${CloudFrontInvalidationFunction}"
            RetentionInDays: 14

    CloudFrontInvalidationFunction:
        Type: "AWS::Serverless::Function"
        Properties:
            Runtime: "provided.al2"
            CodeUri:
                # put your source bucket
                Bucket: "your-bucket"
                Key: "cform-cloudfront-invalidation.zip"
            Handler: "pl.wrzasq.cform.cloudfront.invalidation.config.LambdaResourcesFactory"
            MemorySize: 256
            Description: "CloudFront cache invalidation Lambda."
            Timeout: 30
            TracingConfig: "Active"
            Policies:
                -
                    PolicyName: "AllowReportingActionState"
                    PolicyDocument:
                        Version: "2012-10-17"
                        Statement:
                            -
                                Action:
                                    - "codepipeline:PutJobFailureResult"
                                    - "codepipeline:PutJobSuccessResult"
                                Effect: "Allow"
                                Resource:
                                    - "*"
```
