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
    # notice that there is no account Id in this example
    CreatedAccount:
        Type: "WrzasqPl::AWS::Account"
        Properties:
            Name: "dev"
            Email: "you+dev@example.com"
            AdministratorRoleName: "OrganizationAdmin"
            OuId: !GetAtt "OrganizationUnit.Id"

    InvitedAccount:
        Type: "WrzasqPl::AWS::Account"
        Properties:
            Id: "123456789"
            Name: "prod"
            Email: "you+prod@example.com"
            AdministratorRoleName: "OrganizationAdmin"
            OuId: !GetAtt "OrganizationUnit.Id"
```

# Properties

## `Id` - string (physical resource ID)

Account ID. If not specified, new account will be created.

## `Name` (required) - string

Account name.

**Note:** Forces replacement.

## `Email` (required) - string

Administrative e-mail address.

**Note:** Forces replacement.

## `AdministratorRoleName` - string

Name of the role to create with management rights.

**Note:** Forces replacement.

## `OuId` - string

ID of containing organizational unit.

## `Tags` - Tags

Resource tags.

# Output values

## `Arn` - string (ARN format)

Account ARN.

# Important notes

-   Deleting account resource only removes account from organization - account itself still exists and needs to be
    terminated manually.
