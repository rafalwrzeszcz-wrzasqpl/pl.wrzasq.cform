<!---
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# API Gateway

One of the major improvements is streamlining **API Gateway** definition resources. Since single API definition contains
plenty of resource, many things need to be repeated. Macro handles dedicated structure that keeps entre API definition
in single structure and extracts it into separate resources also making some tedious tasks automated or at least
simplified.

## Definition

Since REST API definition is combining many other resources it is separated into new template section `RestApis` - it is
a hash with API logical identifier (key in your template) as a key and API properties as each entry:

```yaml
RestApis:
    MyApi:
        Authorizers:
            # authorizers definitions (see below)
        
        Models:
            # models definitions (see below)
        
        Resources:
            # API structure definition (see below)

        # all other properties will be mapped directly to API resources, so you can do:
        Name: "my-public-api"
```

Each REST API also defines `AWS::ApiGateway::Deployment` resource.

Defining any API using `RestApis` block also creates `AWS::ApiGateway::Account` and logging role for it.

**Note:** On each level any property not mentioned directly here is passed to underlying resource, so in case of any
changes in native **CloudFormation** resources can be directly used in the macro.

### Authorizers

```yaml
Authorizers:
    SomeAuthorizerId:
        Type: "TOKEN"
        # this is not `!Sub`, this is an API gateway stage variable reference
        Lambda: "${stageVariables.AuthorizerLambda}"
        IdentitySource: "method.request.header.Authorization"
        AuthorizerResultTtlInSeconds: 3600
```

This is a completely ordinary authorizer definition except that it is automatically scoped within API. You also don't
need to define its name.

**Note:** For `Lambda` property see "Simplifications" section.

### Models

```yaml
Models:
    AccountInfo:
        Schema:
            type: "object"
            properties:
                accountName:
                    type: string
            required:
                - "accountName"
            additionalProperties: false
```

Again, definition of model is just a native `AWS::ApiGateway::Model` just automatically scoped to current API. Content
type, schema type and model title are, however, optional. You can see "Simplifications" section for details.

### API structure

The biggest simplification comes from converting excessive definitions of resource resources (well… resources of
resources?) into structure definition - a little like Open API directly in CloudFormation template:

```yaml
# keep in mind - this is `Resources` section of API not a top-level template section
Resources:
    /accounts:
        /{accountId}:
            "@GET":
                # integration method - see below
            
            "@DELETE":
                # integration method - see below
    /keys:
        "@GET":
            # integration method - see below
```

There is literally no resource definition needed, as it is extracted directly from the tree-ish structure. It is just a
container for integration methods definitions.

### Integration

```yaml
"@PUT":
    Authorizer: "TokenAuthorizer"
    RequestValidator: "BODY_AND_PARAMETERS"
    RequestModels:
        application/json: !Ref "RestApi:V1:Model:TenantInfo"
    RequestParameters:
        method.request.header.Authorization: true
        method.request.path.clientTenantId: true
        method.request.path.apiKey: true
    Integration:
        Lambda: "${stageVariables.TenantKeySavingTarget}"
        Credentials: !GetAtt "ApiGatewayRole.Arn"
        RequestTemplates:
            application/json: |
                {
                    "tenantId": "$context.authorizer.tenantId",
                    "clientTenantId": "$input.params('clientTenantId')",
                    "apiKey": "$input.params('apiKey')",
                    "tenantInfo": $input.json('$')
                }
        IntegrationResponses:
            "202":
                ResponseParameters:
                    method.response.header.Content-Type: "'application/atom+xml'"
            "404":
                SelectionPattern: ".*Tag not found.*"
        PassthroughBehavior: "NEVER"
```

-   So, again many of these is the same as `AWS::ApiGateway::Method` definition with some changes.
-   Method integration is automatically scoped to the API and containing resource and method is set to key where it is
    defined - for `@PUT` its set to `PUT`.
-   `Authorizer` (instead of `AuthorizerId`) is just an identifier from `Authorizers` definitions - macro converts it to
    proper reference. `AuthorizerType` is also automatically assigned based on referred authorizer type.
-   `RequestValidator` (instead of `RequestValidatorId`) is just one of the three values: `BODY_ONLY`,
    `PARAMETERS_ONLY, or `BODY_AND_PARAMETERS`. Underlying validators are managed on-demand by macro.
-   Structure of `MethodResponses` and `Integration.IntegrationResponses` can be defined as maps, where keys become
    `StatusCode` of defined responses (you can still use native notation of CloudFormation template).
-   If no `MethodResponses` is defined directly in the method it is built based on `Integration.IntegrationResponses` by
    leaving `StatusCode` and `ResponseParameters` (if present) replaced by boolean flags instead of any values.

**Note:** Mapping keys in CloudFormation templates must be strings, even if it's `202` it must be wrapped within quotes!

## References

You can refer to any resource defined by an API using specific notation (macro replaces them in `Ref`, `Fn::Sub` and
`Fn::GetAtt`):

-   `RestApi:MyApi` - returns API reference;
-   `RestApi:MyApi:Deployment` - current API reference deployment;
-   `RestApi:MyApi:Authorizer:AuthorizerIdentifier` - authorizer reference;
-   `RestApi:MyApi:Validator:ValidatorIdentifier` - validator reference (it can be `BODY_ONLY`, `PARAMETERS_ONLY` or
    `BODY_AND_PARAMETERS`);
-   `RestApi:MyApi:Model:ModelIdentifier` - model reference;
-   `RestApi:MyApi:Resource:ResourcePath` - resource reference;
-   `RestApi:MyApi:Method:MethodAndResourcePath` - integration method reference.

In case of `Resource` and `Method` references you need to use `%param%` instead of `{param}` path placeholder as that
would collide with `Fn::Sub` calls.

Examples of references:

-   `!Ref "RestApi:MyApi:Authorizer:TokenAuthorizer"`
-   `!GetAtt "RestApi:MyApi.RootResourceId"`
-   `!Sub "${RestApi:MyApi:Resource:/%accountId%}"`

## Simplifications

-   For integration `Type` defaults to `AWS` and `IntegrationHttpMethod` defaults to `POST`.
-   In integration methods and authorizers you can use `Lambda` property instead of `Uri` and `AuthorizerUri`
    respectively - for that you can define just a Lambda function name (can be a reference or even stage variable).
-   Model content-type defaults to `application/json`, it's `$schema` to `http://json-schema.org/draft-04/schema#` and
    `title` to its template identifier.
-   You don't need to define validators - macro will define them as they are needed basing on validation option picked
    by a resource method integration.
-   `Name` of validator is not required - it defaults to its template identifier.

## Full example

```yaml

RestApis:
    # ApiGatewayV1
    V1:
        Name: "MyTestApi"
        Authorizers:
            TokenAuthorizer:
                Type: "TOKEN"
                Lambda: "${stageVariables.AuthorizerLambda}"
                IdentitySource: "method.request.header.Authorization"
                AuthorizerResultTtlInSeconds: 3600
        Models:
            AccountInfo:
                Schema:
                    type: "object"
                    properties:
                        accountName:
                            type: string
                    required:
                        - "accountName"
                    additionalProperties: false
        Resources:
            /accounts:
                /{accountId}:
                    "@DELETE":
                        Authorizer: "TokenAuthorizer"
                        RequestValidator: "PARAMETERS_ONLY"
                        RequestParameters:
                            method.request.header.Authorization: true
                            method.request.path.accountId: true
                        Integration:
                            Lambda: "${stageVariables.AccountDeletionTarget}"
                            Credentials: !GetAtt "ApiGatewayRole.Arn"
                            RequestTemplates:
                                application/json: |
                                    {
                                        "tenantId": "$context.authorizer.tenantId",
                                        "accountId": "$input.params('accountId')"
                                    }
                            IntegrationResponses:
                                "202": {}
                            PassthroughBehavior: "NEVER"

                    /keys:
                        /{apiKey}:
                            "@PUT":
                                Authorizer: "TokenAuthorizer"
                                RequestValidator: "BODY_AND_PARAMETERS"
                                RequestModels:
                                    application/json: !Ref "RestApi:V1:Model:AccountInfo"
                                RequestParameters:
                                    method.request.header.Authorization: true
                                    method.request.path.accountId: true
                                    method.request.path.apiKey: true
                                Integration:
                                    Lambda: "${stageVariables.AccountKeySavingTarget}"
                                    Credentials: !GetAtt "ApiGatewayRole.Arn"
                                    RequestTemplates:
                                        application/json: |
                                            {
                                                "tenantId": "$context.authorizer.tenantId",
                                                "accountId": "$input.params('accountId')",
                                                "apiKey": "$input.params('apiKey')",
                                                "accountInfo": $input.json('$')
                                            }
                                    IntegrationResponses:
                                        "202": {}
                                    PassthroughBehavior: "NEVER"

Outputs:
    RestApiId:
        Description: "API Gateway ID."
        Value: !Ref "RestApi:V1"
```
