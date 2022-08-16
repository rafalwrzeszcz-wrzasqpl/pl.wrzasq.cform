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
    TenantItem:
        Type: "WrzasqPl::AWS::DynamoDbItem"
        Properties:
            TableName: "tenants"
            Id: "tenant0"
            Key:
                tenantId:
                    S: "wrzasq.pl"
            Data:
                accountName:
                    S: "Rafał Wrzeszcz - Wrzasq.pl"
```

**Note:** Resource handle actually doesn't check what is the key definition in the table - separation is to maintain
**CloudFormation** identity checks to track changes in the resources.

**Note:** Property `Id` is only introduced for satisfying requirements of integration schema (`Key` can not be used as
only string attributes can be used for identifiers). To avoid orphan items you should change `Id` every time you change
`Key`.

# Properties

## `TableName` (required) - string (physical resource ID)

Target table name.

**Note:** Forces replacement.

## `Id` (required) - string (physical resource ID)

Unique record identifier.

**Note:** Forces replacement.

## `Key` (required) - object

Item key structure.

## `Data` - object

Any additional data fields in the item.

# Output values

## `Item` - object

Structure of recorded item.
