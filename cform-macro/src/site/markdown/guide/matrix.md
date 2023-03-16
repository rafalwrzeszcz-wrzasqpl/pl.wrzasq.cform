<!---
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2023 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Matrix resources

One of the major difficulties in creating flexible and re-usable **CloudFormation** template is lack of dynamic
resources creation, especially depending on some variable inputs. Using this macro one can specify matrix of variable
inputs for some resource, and it will be replicated for each variation expanding input template resource into multiple
resources dynamically (but in predictable way to avoid stack drifts and unnecessary infrastructure changes).

## Definition

To turn resource definition into matrix resource simply add `Matrix` definition to it (note that unlike some other
"major" features of the macro, here we don't introduce any new section of the template - we operate directly within
`Resources` section):

```yaml
Resources:
    TasksQueue:
        Type: "AWS::SQS::Queue"
        # this is it
        Matrix:
            Entries:
                # you can define multiple parameters - matrix will be generated for each combination
                Env:
                    Prod: "prod01"
                    Dev: "dev"
                Task:
                    Customers: "customers"
                    Order: "orders-v2"
        Properties:
            ContentBasedDeduplication: true
```

Declaration above will result in 4 resources in processed template:
- `Prod` for `Customers`
- `Prod` for `Orders`
- `Dev` for `Customer`
- `Dev` for `Orders`

There are different ways in which you can specify input values for some matrix parameter. It may be important to pick
one that fits not only your input definition structure, but also resources management pattern, as it affects how final
resources are organised in the template.

Each matrix variant is mapped using parameter key, which is obtained in different way depending on specified structure.

Each matrix entry value on the other hand can be used in properties resolution (will be described later).

### Dict

A dictionary (map) is shown in the above example. Each entry has a key, which will be used in resource identification.
Mentioned example (simplified):

```yaml
Resources:
    TasksQueue:
        Type: "AWS::SQS::Queue"
        Matrix:
            Entries:
                Env:
                    Prod: "prod01"
                    Dev: "dev"
```

Will result in having the following resources:
- `TasksQueueProd`
- `TasksQueueDev`

### List

If only values are important for us (how to handle them will be described in next section) a list can be used:

```yaml
Resources:
    TasksQueue:
        Type: "AWS::SQS::Queue"
        Matrix:
            Entries:
                Env:
                    - "prod01"
                    - "dev"
```

In case of list, key is a numeric index, so in this case we will have the following resources:
- `TasksQueue0`
- `TasksQueue1`

Important thing is that this way is essentially same mechanism as described below reference to a parameter, so it can be
used for mocking multiple resources before knowing particular input interface. Because logical IDs of final resources
are generated in predictable way, it can be later swapped without causing any changes in stack.

### Parameter reference

The last way to define input values is referring to an input parameter. Parameter will be treated as a comma-separated
set of values (regardless of parameter type):

```yaml
Parameters:
    Envs:
        # it doesn't matter, but of course you can use `CommaDelimitedList` for better visibility
        Type: "String"
        Default: "dev,prod"

Resources:
    TasksQueue:
        Type: "AWS::SQS::Queue"
        Matrix:
            Entries:
                Env: "Envs"
```

**Note:** There is no `!Ref` call! Specifying single string results in resolving a parameter, defining `!Ref: "Envs"`
effectively passes a map.

## LogicalIdPattern

Ok, so this is how we can define matrix of resources. But it's very important to understand how will it generate
results, as we are speaking about infrastructure - and especially CloudFormation is very sensitive to changes in
template structure or resource identifiers.

Each resource will have it's unique logical ID generated based on pattern, so it will be predictable and particular
entries in matrix should be identifiable. By default, each matrix parameter adds it's key to the pattern, so in our
initial example logical ID will have form of: `TasksQueue${Env}${Task}`. If you need different pattern, you can set it
using `LogicalIdPattern` property:

```yaml
Resources:
    TasksQueue:
        Type: "AWS::SQS::Queue"
        Matrix:
            # here - note that there is no call it any CloudFormation function, like `!Sub`, this is resolved by macro
            # based on matrix definition, not stack parameters - you even can have a parameter named `Env` in the stack,
            # and it won't be affecting how logical IDs are generated
            LogicalIdPattern: "QueueFor_${Env}"
            Entries:
                Env:
                    Prod: "prod01"
                    Dev: "dev"
```

Example above will generate queues with logical IDs `QueueFor_Prod` and `QueueFor_Dev`.

## Substitutions

Having resources identification handled we can move to second part - values resolving. Matrix definition would be most
likely useless without possibility to define values dependent on each option. When replicating matrix resources all
string values in `Properties` (that includes nested structures!) are processed looking for `${Each:}` references.

**Note:** This is not a call to any CloudFormation function, it is done implicitly by macro on each string value. And
unlike logical ID, which is only used for static string generation, values within `Properties` may have calls to
functions, so we use `Each:` prefix to make it explicit.

Knowing that, let's try to enhance our example and define some tags for the queue:

```yaml
Resources:
    TasksQueue:
        Type: "AWS::SQS::Queue"
        Matrix:
            Entries:
                Env:
                    Prod: "prod01"
                    Dev: "dev"
                Task:
                    Customers: "customers"
                    Order: "orders-v2"
        Properties:
            ContentBasedDeduplication: true
            Tags:
                -
                    Key: "env"
                    Value: "${Each:Env}"
                -
                    Key: "id"
                    # note the function call - it will remain and only ${Each:} placeholder will be pre-processed
                    Value: !Sub "${AWS::AccountId}-${Each:Task}"
```

Output will be as follows:

```yaml
Resources:
    TasksQueueDevCustomers:
        Type: "AWS::SQS::Queue"
        Properties:
            ContentBasedDeduplication: true
            Tags:
                -
                    Key: "env"
                    Value: "dev"
                -
                    Key: "id"
                    Value: !Sub "${AWS::AccountId}-customers"

    TasksQueueDevOrder:
        Type: "AWS::SQS::Queue"
        Properties:
            ContentBasedDeduplication: true
            Tags:
                -
                    Key: "env"
                    Value: "dev"
                -
                    Key: "id"
                    Value: !Sub "${AWS::AccountId}-orders-v2"

    TasksQueueProdCustomers:
        Type: "AWS::SQS::Queue"
        Properties:
            ContentBasedDeduplication: true
            Tags:
                -
                    Key: "env"
                    Value: "prod01"
                -
                    Key: "id"
                    Value: !Sub "${AWS::AccountId}-customers"

    TasksQueueProdOrder:
        Type: "AWS::SQS::Queue"
        Properties:
            ContentBasedDeduplication: true
            Tags:
                -
                    Key: "env"
                    Value: "prod01"
                -
                    Key: "id"
                    Value: !Sub "${AWS::AccountId}-orders-v2"
```

## References

Last part of this feature covers resolving references. Of course knowing particular ID patterns it would be possible to
write things like `!Ref: "TasksQueueDevOrder"`. But this way may be problematic if you suddenly change the pattern of
the ID. To make a reference to any matrix element you can use special notation (it will work in `Ref`, `Fn::GetAtt` and
`Fn::Sub` calls) - `Matrix:MatrixId[Param1=Key1,Param2=Key2]`. You can also add attribute name after dot - it will just
be kept for underlying replaced call.

Some examples:

-   `!Ref "Matrix:TasksQueue[Env=Dev,Task=Customers]"` will be replaced with `!Ref "TasksQueueDevCustomers"`
-   `!GetAtt "Matrix:TasksQueue[Env=Prod,Task=Customers].Arn"` will be replaced with `!GetAtt
    "TasksQueueProdCustomers.Arn"`
-   `!Sub "Queue name is ${Matrix:TasksQueue[Env=Prod,Task=Order].QueueName}"` will be replaced with `!Sub "Queue name
    is ${TasksQueueProdOrder.QueueName}`

In many cases this structure can be over-verbose, but it can be used to reliably address always proper matrix entry.

### Length

Additionally, it's possible to also obtain size of the matrix by using `Fn::Length` call (keep in mind this function
requires
[AWS::LanguageExtension transform](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-length.html)
in your template). For that, a shortened notation without element selector is used - just `Matrix:MatrixId`:

```yaml
Outputs:
    MatrixSize:
        Value:
            "Fn::Length": "Matrix:TasksQueue"
```
