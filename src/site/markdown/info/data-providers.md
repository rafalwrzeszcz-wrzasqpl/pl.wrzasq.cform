<!---
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

Some resource providers in **WrzasqPl-CForm** are _data providers_. It means they do not manage resources but instead
load data about existing ones (you can think of them in similar way as in
[Terraform](https://www.terraform.io/docs/language/data-sources/index.html)).
[CloudFormation](https://aws.amazon.com/cloudformation/) doesn't provide such a concept, so they are implemented as
regular resource providers and defined as regular resources in your templates, but they do not create anything and will
not cause any changes - instead it accesses information about existing resources. Only parameters that the data provider
accepts is the resource identifier.

Keep in mind that whenever possible, you should use outputs of your managed records (if you access properties of
different stack resources you should use
[cross-stack references](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/walkthrough-crossstackref.html)
). Data providers are implemented for properties that are not exposed by default resources.
