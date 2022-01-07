<!---
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Lambda@Edge configuration

When using **Lambda**, it's very common to use environment variables to expose configuration. This allows for making
generic and re-usable code that can be configured at deploy time.

Unfortunately **Lambda@Edge** doesn't support environment variables. That means that the code needs to be entirely
encapsulated in deployment package. This makes configuration-at-deploy-time impossible.

This is also targeted with `lambda-edgedeploy` handler. When deploying function for use by **CloudFront**, handler
opens specified package and injects configuration passed to it as **JSON** file. By default, it's named `config.json`,
but name is also configurable.

Let's say you want to expose some build variables to your function via **CloudFormation**:

```yaml
    EdgeFunction:
        Type: "WrzasqPl::AWS::EdgeDeploy"
        Properties:
            # … all of the Lambda@Edge properties here …
            PackageBucket: "deploy-bucket"
            PackageKey: "path/to/your/lambda.zip"
            # by default config.json is used as filename
            ConfigFile: "env.json"
            Config:
                # you can use references to other resources and parameters
                # these are just a regular CloudFormation-processed values
                applicationVersion: !Ref "ReleaseVersion"
                applicationDomain:
                    "Fn::ImportValue": !Sub "${ProjectKey}:${ProjectVersion}:DomainName"
                facebookAppId: !Ref "FacebookAppId"
```

This will deploy your `lambda.zip` package with `env.json` file in it with configuration values:

```json
{
    "applicationVersion": "0.0.1",
    "applicationDomain": "wrzasq.pl",
    "facebookAppId": "123456789abcdefgh"
}
```

## Using within Node.js

**Note:** Keep in mind that **Lambda@Edge** can only handle **Node.js** packages, so there is no reason to explain
implementations for other runtimes.

As **JSON** is a native notation of **JavaScript**, it's very easy to load built config file within your Node Lambda:

```js
// require() style
let env = require("env.json");

// ES import style
import * as env from "./env.json";
```
