 <!---
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# CloudFormation template

Once you have registered type handler (see deployment guide), you can use resource in your templates:

```yaml
Resources:
    RewritesFunction:
        Type: "WrzasqPl::AWS::EdgeDeploy"
        Properties:
            Name: "edge-feeds-rewrites"
            RoleArn: !GetAtt "RewritesLambdaRole.Arn"
            Handler: "index.handler"
            Runtime: "nodejs14.x"
            Memory: 128
            Timeout: 5
            PackageBucket: "your-bucket"
            PackageKey: "input-lambda.zip"
            Config:
                rewrites:
                    -
                        pattern: "^/tag,([a-zA-Z0-9-_]+)\\.atom$"
                        rewrite: "/feeds/tags/$1"
                    -
                        pattern: "^/([a-zA-Z0-9-_]+)\\.atom$"
                        rewrite: "/feeds/categories/$1"
```

# Properties

## `Name` (required, physical resource ID) - string

Account name.

**Note:** Forces replacement.

## `Description` - string

Lambda function description.

## `RoleArn` (required) - string (ARN format)

ARN of Lambda execution role.

## `Runtime` (required) - string

Runtime for running the Lambda (note that Lambda@Edge has reduced set of supported runtimes).

## `Handler` (required) - string

Lambda entry point.

## `Memory` (required) - integer

Memory size (in MB) for the Lambda.

## `Timeout` (required) - integer

Lambda timeout (in seconds).

## `PackageBucket` (required) - string

Package S3 bucket.

## `PackageKey` (required) - string

Package S3 key.

## `ConfigFile` - string

Filename for the injected configuration.

Defaults to `config.json`.

## `Config` - object

Custom configuration to bundle with the package.

## `Tags` - Tags

Resource tags.

# Output values

## `Arn` - string (ARN format)

Account ARN.

# Deleting replicas

Even though **Lambda@Edge** can only be managed from `us-east-1` region, functions deployed as **Lambda@Edge** (bound
to **CloudFront** distribution) are being replicated across all regions in **AWS** when only they are used in other
location.

Unfortunately [replicas can't be deleted](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/lambda-edge-delete-replicas.html)
manually. This will cause automated resource deletion, triggered by **CloudFormation** to fail.

This will not break stack update process, as unused resources are deleted in `UPDATE_COMPLETE_CLEANUP_IN_PROGRESS`
phase, but will leave the function in `us-east-1` region deployed. You will have to wait until all replicas are purged
(usually few hours) and manually delete.
