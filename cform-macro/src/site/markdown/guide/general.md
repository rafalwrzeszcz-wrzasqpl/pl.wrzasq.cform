<!---
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

Apart from big things like API and pipeline definitions macro also handles many minor things that simplifies common
code duplication parts in many **CloudFormation** templates. This page presents list of changes that macro applies.

However, main rule when working on macro transformations is to keep it transparent. For all of these cases native
**CloudFormation** notations are supported and only in case when simplified notation is detected the transformation is
applied. In principle, it is possible to include macro in any template, and it should just work transparently.

Keep in ming that despite all examples are presented in **YAML** notation you can use them also in **JSON** - macro is
applied to template structure not its document. In some places the conventions needed to be picked to ensure unambiguity
and predictability. These conventions differ from native CloudFormation conventions intentionally to avoid collisions
and predictability is highly wanted mainly to avoid unattended template changes which would result in physical resources
changes.

**Note:** Macro handlers work as a pre-processors, they do not have any execution-time information - they operate on
plain template structure so there are certain limitations to it. Macro is just a template transformation which modifies
it before executing by CloudFormation.

# Simplifications

Primarily, there are some simplifications that reduces verbosity of the template source code mapping some simple
properties into their full representations.

## CodeBuild

-   If you specify `Cache` as plain value (string or `Fn::` call) it will be expanded into `S3` type with `Location`
    taken as the value:
    ```yaml
    CachedProject:
        Type: "AWS::CodeBuild::Project"
        Properties:
            ServiceRole: !ImportValue "root:v1:codebuild:role:name"
            Environment:
                Image: "maven:3.6.2-jdk-11"
            # will be expanded into { "Type": "S3", "Location": !Sub "${CacheBucketName}/integrations" }
            Cache: !Sub "${CacheBucketName}/integrations" 
    ```
-   `Artifacts` section will be automatically set to `S3` if there is a `Location` property:
    ```yaml
    PipelineProject:
        Type: "AWS::CodeBuild::Project"
        Properties:
            ServiceRole: !ImportValue "root:v1:codebuild:role:name"
            Environment:
                Image: "maven:3.6.2-jdk-11"
            Artifacts:
                Location: !ImportValue "root:v1:codepipeline:artifacts-bucket:name"
                Path: !Ref "ProjectName"
                Name: "checkout.zip"
                Packaging: "ZIP"
                # this is not needed
                # Type: "S3"
    ```
-   Plain `EnvironmentVariables` can be specified as a mapping:
    ```yaml
    PipelineProject:
        Type: "AWS::CodeBuild::Project"
        Properties:
            Environment:
                EnvironmentVariables:
                    ServiceUrl: "https://example.com"
                    ServiceVersion: "v1"
    ```

## DynamoDb

-   All key attributes (both from the table itself and secondary indices) that are not in `AttributeDefinitions` schema
    are added automatically with type `S`:
    ```yaml
    SomeTable:
        Type: "AWS::DynamoDB::Table"
        DeletionPolicy: "Retain"
        Properties:
            AttributeDefinitions:
                -
                    AttributeName: "version"
                    AttributeType: "N"
                # clientId and apiKey will be added automatically with Type: "S"
            KeySchema:
                -
                    AttributeName: "clientId"
                    KeyType: "HASH"
                -
                    AttributeName: "version"
                    KeyType: "RANGE"
            GlobalSecondaryIndexes:
                -
                    IndexName: "keys"
                    KeySchema:
                        -
                            AttributeName: "clientId"
                            KeyType: "HASH"
                        -
                            AttributeName: "apiKey"
                            KeyType: "RANGE"
    ```

## IAM

-   Property `Policies` in `AWS::IAM::Group`, `AWS::IAM::Role` and `AWS::IAM::User` can be written in more compat way as
    a mapping from policy name to policy document and policy document gets wrapped with `Statement` and `Version`
    envelope:
    ```yaml
    ApiLambdaRole:
        Type: "AWS::IAM::Role"
        Properties:
            AssumeRolePolicyDocument:
                -
                    Action: "sts:AssumeRole"
                    Effect: "Allow"
                    Principal:
                        Service:
                            - "lambda.amazonaws.com"
            ManagedPolicyArns:
                - "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
                - "arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess"
            Policies:
                AllowUsingSomeTable:
                    -
                        Action:
                            - "dynamodb:Query"
                        Effect: "Allow"
                        Resource:
                            - !Sub "${SomeTable.Arn}/index/keys"
    ```
-   Property `PolicyDocument` in `AWS::IAM::ManagedPolicy` and `AWS::IAM::Policy` and `AssumeRolePolicyDocument` in
    `AWS::IAM::Role` are expanded same way (as a single policy statement).

# Automatic log group

For `AWS::Lambda::Function`, `AWS::Serverless::Function` and `AWS::CodeBuild::Project` resources you can specify new
property - `LogsRetentionInDays`. When it is detected, corresponding `AWS::Logs::LogGroup` resource is automatically
defined with name that follows convention to provision log group that will be used by the function or build project.

**Note:** Log group can not exist at the moment of template execution, otherwise **CloudFormation** will fail to
provision it. If you already have such log group, you need to manually delete it, or import existing log group into the
stack.

# `Fn::ImportValue` within `Fn::Sub`

[`Fn::Sub`](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-sub.html) was a
big simplification on its own when it was introduced - before that you had to handle each string concatenation with
spaghetti `Fn::Join` call with empty separator. The simplest case is when you can put all your placeholders into string
parameters for further interpolation - eg.
`!Sub "arn:aws:execute-api:${AWS::Region}:${AWS::AccountId}:${RestApiId}/${ApiStage}/${Table.Arn}"`. If you use
something else than simple references or attributes you unfortunately need to switch to more verbose structure:

```yaml
SomeProperty:
    "Fn::Sub":
        - "Your ${Message}"
        -
            Message: !ImportValue "RootBucketName"
```

This is very common in case of `Fn::ImportValue` inserted into `Fn::Sub`. With the macro you can interpolate import
values directly into pattern string to simplify the notation just the same way as replacements for `Ref` and `Fn::Sub`
calls - all placeholders beginning with `Import:` prefix are replaced with placeholders backed by `Fn::ImportValue`:

```yaml
SomeProperty: !Sub "Your ${Import:RootBucketName}"
```

# Sensible defaults

Additionally, some vales are specified as defaults - these are the cases when the values are required by
**CloudFormation** anyway, so if you use the following setup presented here, you can omit it and it will be applied
implicitly.

## CodeBuild

-   If you don't specify `Source` or `Artifacts` property of the project it will be set to `CODEPIPELINE` type.
    ```yaml
    # this effectively defines project for CodePipeline
    PipelineProject:
        Type: "AWS::CodeBuild::Project"
        Properties:
            ServiceRole: !ImportValue "root:v1:codebuild:role:name"
            Environment:
                Image: "maven:3.6.2-jdk-11"
    ```
-   Environment build type is by default set to `LINUX_CONTAINER` and compute type to `BUILD_GENERAL1_SMALL`.
