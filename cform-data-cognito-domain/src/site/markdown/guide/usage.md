<!---
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# CloudFormation template

Once you have registered type handler (see deployment guide), you can use resource in your templates:

```yaml
Resources:
    CognitoDomain:
        Type: "WrzasqPl::Cognito::DomainData"
        Properties:
            Domain: "yourcompany"

    Route53Binding:
        Type: "AWS::Route53::RecordSet"
        Properties:
            HostedZoneId: !Ref "YourHostedZone"
            Name: "auth.wrzasq.pl."
            Type: "A"
            AliasTarget:
                HostedZoneId: "Z2FDTNDATAQYW2"
                DNSName: !GetAtt "CognitoDomain.CloudFrontDistribution"
```

## Data provider

This resource type is a data provider - it accesses existing resource to expose its properties in the template.

To create domain resource use
[`AWS::Cognito::UserPoolDomain`](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-cognito-userpooldomain.html)
resource.

# Properties

## `Domain` (required) - string (physical resource ID)

Domain used by **Cognito**.

# Output values

## `CloudFrontDistribution` - string

CloudFront distribution DNS name of **Cognito** domain.
