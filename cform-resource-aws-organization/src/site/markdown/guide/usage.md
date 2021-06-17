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
    Test:
        Type: "WrzasqPl::AWS::Organization"
        Properties:
            FeatureSet: "ALL"
```

# Properties

## `FeatureSet` (required) - string

Specifies set of features enabled for accounts in organization. Can be either `CONSOLIDATED_BILLING` or `ALL`.

**Note:** It only applies during organization creation, modification is not possible.

# Output values

## `Id` - string (physical resource ID)

Organization ID.

## `Arn` - string (ARN format)

Organization ARN.

## `RootId` - string

Root organizational unit ID.
