<!---
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2021 - 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Serverless repository

Macro is available in AWS Serverless Repository as [`wrzasqpl-cform-macro` application](https://eu-central-1.console.aws.amazon.com/lambda/home?region=eu-central-1#/create/app?applicationId=arn:aws:serverlessrepo:eu-central-1:117504620086:applications/wrzasqpl-cform-macro).

## CloudFormation deployment

You can deploy it using **CloudFormation** by referring to repository:

```yaml
WrzasqPlCformMacro:
    Type: "AWS::Serverless::Application"
    Properties:
        Location:
            ApplicationId: "arn:aws:serverlessrepo:eu-central-1:117504620086:applications/wrzasqpl-cform-macro"
            SemanticVersion: "1.1.12" # check for the latest tag
        # `Parameters` section is optional, default name is `WrzasqPlCformMacro` - you can define custom name here
        Parameters:
            MacroName: "NameItCformMacro"
```

# CloudFormation manual deployment

Alternatively, if you need full control over artifacts, you may handle the process entirely on your side - you need to
upload packaged macro handler (`cform-macro.zip` from
[GitHub Releases](https://github.com/rafalwrzeszcz-wrzasqpl/pl.wrzasq.cform/releases)) to your **S3** bucket.
Afterwards you can just execute following template:

```yaml
Resources:
    LogGroup:
        Type: "AWS::Logs::LogGroup"
        Properties:
            LogGroupName: !Sub "/aws/lambda/${MacroFunction}"
            RetentionInDays: 14

    MacroFunction:
        Type: "AWS::Serverless::Function"
        Properties:
            Runtime: "provided.al2"
            CodeUri:
                # put your source bucket
                Bucket: "your-bucket"
                Key: "cform-macro.zip"
            Handler: "pl.wrzasq.cform.macro.config.LambdaResourcesFactory"
            MemorySize: 256
            Description: "pl.wrzasq.cform CloudFormation macro handler."
            Timeout: 30
            TracingConfig: "Active"

    Macro:
        Type: "AWS::CloudFormation::Macro"
        Properties:
            # name it however you want
            Name: "WrzasqPlCForm"
            FunctionName: !GetAtt "MacroFunction.Arn"
```
