<!---
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# CodePipeline

Building automation pipeline is a crucial part of DevOps approach - **CodePipeline** is the natural choice in **AWS**
ecosystem. Unfortunately expressing it in **CloudFormation** very often requires verbose syntax and manual handling of
all aspects (as usual in CloudFormation). This macro makes at least some of them transparent, implicit or automated.

## Definition

The main part of pipeline definition is its structure. There are few changes to the standard CloudFormation notation
of `AWS::CodePipeline::Pipeline`:

-   within each stage actions are defined as mapping, with action names as keys;
-   `RunOrder` is defined automatically based on artifacts and namespace variables used.

If you define structure in the normal way, macro will simply not apply processing leaving your plain definition.

**Note:** Stage definitions order matters as it defines execution order, but on actions level it is not important as by
default all actions are executed in parallel and in case of dependencies order is managed with `RunOrder` property.

## Conditions

In each stage and action you can optionally define property `Condition` that will turn given stage or action definition
into conditional statement - this property prevents from changing pipeline structure keeping all the entries at the same
levels:

```yaml
    DeployPipeline:
        Type: "AWS::CodePipeline::Pipeline"
        Properties:
            Stages:
                -
                    Name: "Checkout"
                    Actions:
                        S3:
                            Condition: "HasCheckout"
                            ActionTypeId:
                                Category: "Source"
                                Owner: "AWS"
                                Provider: "S3"
                                Version: "1"
                            Configuration:
                                S3Bucket: !ImportValue "root:v1:codepipeline:artifacts-bucket:name"
                                S3ObjectKey: !Sub "${ProjectName}/checkout.zip"
                            OutputArtifacts:
                                -
                                    Name: "checkout"
                        Git:
                            # …
                -
                    Name: "Promote"
                    Condition: "HasNextStage"
                    Actions:
                        # …
```

**Note:** Keep in mind that this is CloudFormation condition - condition won't be evaluated during pipeline execution
but at the moment of pipeline creation time, so it will be ether always defined or always absent.

## Actions

Currently, following action types have simplified definition (using `ActionType` property instead of `ActionTypeId`).

### CloudFormationDeploy

Simplification of CloudFormation `CREATE_UPDATE` action mode:

```yaml
Actions:
    DeployStack:
        Type: "CloudFormationDeploy"
        Configuration:
            StackName: !Sub "${AWS::StackName}-api"
            RoleArn: !GetAtt "InfrastructureRole.Arn"
            TemplatePath: "checkout::infrastructure/cloudformation/api.yaml"
            TemplateConfiguration: "checkout::infrastructure/cloudformation/config-root.json"
        Parameter:
            ProjectVersion: !Ref "ProjectVersion"
            HostedZoneId: "#{PreviousStage:DNS.HostedZoneId}"
```

-   Dependencies are detected by references and will be used to compute `RunOrder` dynamically.
-   You don't need to specify `Namespace`, it will be resolved by a macro.
-   `Capabilities` property is set to `CAPABILITY_NAMED_IAM,CAPABILITY_AUTO_EXPAND`.
-   Artifacts used for `TemplatePath` and `TemplateConfiguration` are automatically added to input artifacts list.
-   Parameters are specified as `Parameters` property directly on action level in a structured way instead of blob.

### CodeBuild

This type simplifies **CodeBuild** project integration:

```yaml
Actions:
    RunProject:
        Type: "CodeBuild"
        Project: !Ref "BuildProject"
```

### S3Deploy

Pipeline source that saves artifact to S3:

```yaml
Actions:
    Checkout:
        Type: "S3Deploy"
        Bucket: !Ref "ArtifactsBucket"
        ObjectKey: "checkout.zip"
        InputArtifacts:
            - "checkout"
```

### S3Promote

This type is a specific case of S3 deployment that is dedicated for multi-stage pipelines - it automatically maps other
action of type `S3Source` (see below) to other bucket.

```yaml
Actions:
    Promote:
        Type: "S3Promote"
        Source: "StageName:CheckoutAction"
        Bucket: !Ref "NextStageBucket"
```

-   You don't need to specify input artifact for this action - it takes artifacts of referred S3 source action.
-   `ObjectKey` is replicated from source reference.
-   `CannedACL` is set to `"bucket-owner-full-control"`.

### S3Source

Pipeline source that takes S3 object:

```yaml
Actions:
    Checkout:
        Type: "S3Source"
        Bucket: !Ref "ArtifactsBucket"
        ObjectKey: "checkout.zip"
        OutputArtifacts:
            - "checkout"
```

## Simplifications

-   You can use plain value (including references) as `ArtifactStore` or entries in `ArtifactStores` and it will be
    converted to `S3` references:
    ```yaml
    DeployPipeline:
        Type: "AWS::CodePipeline::Pipeline"
        Properties:
            ArtifactStores:
                -
                    Region: !Ref "AWS::Region"
                    ArtifactStore: !ImportValue "root:v1:codepipeline:artifacts-bucket:name"
                -
                    Region: "us-east-1"
                    ArtifactStore: !ImportValue "root:v1:codepipeline:us-east-1-artifacts-bucket:name"

    ```
-   In action's `ActionTypeId` properties `Owner` and `Version` have default values as `"AWS"` and `"1"` respectively.
-   Input and output artifacts can be defined as list of strings:
    ```yaml
    SomeActions:
        InputArtifacts:
            - "checkout"
        OutputArtifacts:
            - "build"
    ```
