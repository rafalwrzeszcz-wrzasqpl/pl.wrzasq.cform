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
    UserPool:
        Type: "AWS::Cognito::UserPool"
        Properties:
            # …

    Client:
        Type: "AWS::Cognito::UserPoolClient"
        Properties:
            UserPoolId: !Ref "UserPoolId"
            # …

    ClientData:
        Type: "WrzasqPl::Cognito::ClientData"
        Properties:
            UserPoolId: !Ref "UserPoolId"
            ClientId: !Ref "Client"

    ManagedSecret:
        Type: "AWS::SecretsManager::Secret"
        Properties:
            SecretString: !Sub |
                {
                    "clientId": "${Client}",
                    "clientSecret": "${ClientData.ClientSecret}"
                }
```

## Data provider

This resource type is a data provider - it accesses existing resource to expose its properties in the template.

To create client resource use
[`AWS::Cognito::UserPoolClient`](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-cognito-userpoolclient.html)
resource.

# Properties

## `UserPoolId` (required) - string (physical resource ID)

**Cognito** user pool ID.

## `ClientId` (required) - string (physical resource ID)

**Cognito** client ID.

# Output values

## `ClientSecret` - string

OAuth client secret.
