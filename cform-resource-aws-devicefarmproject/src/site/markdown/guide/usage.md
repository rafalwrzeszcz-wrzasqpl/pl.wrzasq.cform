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
        Type: "WrzasqPl::AWS::DeviceFarmProject"
        Properties:
            Name: "Selenium project"
            Description: "Functional tests of web application"
```

# Properties

## `Name` (required) - string

Name of the TestGrid project.

## `Description` - string

Project description.

# Output values

## `Arn` - string (ARN format) (physical resource ID)

TestGrid project ARN.
