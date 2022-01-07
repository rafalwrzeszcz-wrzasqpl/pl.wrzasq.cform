<!---
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# CodePipeline integration

Lambda can be invoked as part of **CodePipeline** to automatically clear cache after deployment:

```yaml
Name: "ClearCache"
Actions:
    -
        Name: "Lambda"
        ActionTypeId:
            Category: "Invoke"
            Owner: "AWS"
            Provider: "Lambda"
            Version: "1"
        Configuration:
            FunctionName: "myLambdaFunction"
            UserParameters: "distributionId"
```
