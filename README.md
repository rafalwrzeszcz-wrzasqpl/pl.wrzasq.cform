<!---
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2020 - 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# WrzasqPl-CForm

**WrzasqPl-CForm** is a set of serverless applications that aim to extend
[CloudFormation](https://aws.amazon.com/cloudformation/) capabilities.

[![Build Status](https://github.com/rafalwrzeszcz-wrzasqpl/pl.wrzasq.cform/actions/workflows/build.yaml/badge.svg)](https://github.com/rafalwrzeszcz-wrzasqpl/pl.wrzasq.cform/actions)

Each module is a stand-alone, independent package. Ready to be deployed into **AWS** out of the box.

Keep in mind, that you can use **Lambda** packages regardless of your technology - they are completely independent,
running in [_FaaS_](https://en.wikipedia.org/wiki/Function_as_a_service) environment, communicating through
**JSON**-based interface.

You can use virtually any tool and integrate them with any project. No piece of your code will directly interact with
this source code.

## Resource handlers

### [WrzasqPl::AWS::Account](https://rafalwrzeszcz-wrzasqpl.github.io/pl.wrzasq.cform/cform-resource-aws-account/)

AWS account creation handler.

### [WrzasqPl::AWS::DeviceFarmProject](https://rafalwrzeszcz-wrzasqpl.github.io/pl.wrzasq.cform/cform-resource-aws-devicefarmproject/)

AWS DeviceFarm project handler.

### [WrzasqPl::AWS::Organization](https://rafalwrzeszcz-wrzasqpl.github.io/pl.wrzasq.cform/cform-resource-aws-organization/)

Manage AWS Organizations setup.

### [WrzasqPl::AWS::OrganizationalUnit](https://rafalwrzeszcz-wrzasqpl.github.io/pl.wrzasq.cform/cform-resource-aws-organizationalunit/)

Manage AWS Organizational Unit setup.

### [WrzasqPl::AWS::PasswordPolicy](https://rafalwrzeszcz-wrzasqpl.github.io/pl.wrzasq.cform/cform-resource-aws-passwordpolicy/)

Sets up password policy for AWS account.

## Data providers

### [WrzasqPl::AppSync::GraphQlApiData](https://rafalwrzeszcz-wrzasqpl.github.io/pl.wrzasq.cform/cform-data-appsync-graphqlapi/)

Reads AppSync GraphQl endpoint information.

### [WrzasqPl::Cognito::DomainData](https://rafalwrzeszcz-wrzasqpl.github.io/pl.wrzasq.cform/cform-data-cognito-domain/)

Reads Cognito custom domain information.

# Resources

-   [GitHub page with API documentation](https://rafalwrzeszcz-wrzasqpl.github.io/pl.wrzasq.cform)
-   [Contribution guide](https://github.com/rafalwrzeszcz-wrzasqpl/.github/blob/master/CONTRIBUTING.md)
-   [Issues tracker](https://github.com/rafalwrzeszcz-wrzasqpl/pl.wrzasq.cform/issues)
-   [Maven packages](https://search.maven.org/search?q=g:pl.wrzasq.cform)
-   [Wrzasq.pl @ GitHub](https://github.com/rafalwrzeszcz-wrzasqpl)
-   [Wrzasq.pl @ Facebook](https://www.facebook.com/wrzasqpl)
-   [Wrzasq.pl @ LinkedIn](https://www.linkedin.com/company/wrzasq-pl/)

# Authors

This project is brought to you by [Rafał Wrzeszcz - Wrzasq.pl](https://wrzasq.pl) and published under
[MIT license](https://github.com/rafalwrzeszcz-wrzasqpl/pl.wrzasq.cform/tree/master/LICENSE).

List of contributors:

-   [Rafał "Wrzasq" Wrzeszcz](https://github.com/rafalwrzeszcz) ([wrzasq.pl](https://wrzasq.pl)).
