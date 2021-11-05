<!---
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# CloudFormation deployment

You need to upload packaged macro handler (`cform-macro.zip` from
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
            Handler: "currently_not_used_in_native"
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
