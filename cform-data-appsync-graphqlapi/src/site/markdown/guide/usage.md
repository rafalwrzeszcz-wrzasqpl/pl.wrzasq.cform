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
    AppSyncGraphQlApi:
        Type: "WrzasqPl::AppSync::GraphQlApiData"
        Properties:
            ApiId: !GetAtt "YourApi.ApiId"

    CloudFrontDistribution:
        Type: "AWS::CloudFront::Distribution"
        Properties:
            DistributionConfig:
                Origins:
                    -
                        Id: "graphql"
                        DomainName: !GetAtt "AppSyncGraphQlApi.DomainName"
                        CustomOriginConfig:
                            OriginProtocolPolicy: "https-only"
            # …
```

## Data provider

This resource type is a data provider - it accesses existing resource to expose its properties in the template.

To create domain resource use
[`AWS::AppSync::GraphQLApi`](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-appsync-graphqlapi.html)
resource.

# Properties

## `ApiId` (required) - string (physical resource ID)

**AppSync** API ID.

# Output values

## `DomainName` - string

**AppSync** API domain name.
