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
    TopLevel:
        Type: "WrzasqPl::AWS::OrganizationalUnit"
        Properties:
            Name: "Top level"
            ParentId: !GetAtt "Organization.RootId"

    Nested:
        Type: "WrzasqPl::AWS::OrganizationalUnit"
        Properties:
            Name: "Nested unit"
            ParentId: !GetAtt "TopLevel.Id"
```

# Properties

## `Name` (required) - string

Name of the organizational unit.

## `ParentId` (required) - string

ID of the parent node (another organizational unit or organization root).

**Note:** Forces replacement.

## `Tags` - Tags

Resource tags.

# Output values

## `Id` - string (physical resource ID)

Organizational unit ID.

## `Arn` - string (ARN format)

Organizational unit ARN.
