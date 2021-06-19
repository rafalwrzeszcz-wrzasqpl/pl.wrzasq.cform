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
        Type: "WrzasqPl::AWS::PasswordPolicy"
        Properties:
            MinimumPasswordLength: 8
            RequireLowercaseCharacters: true
            RequireUppercaseCharacters: true
            RequireNumbers: true
            RequireSymbols: true
            AllowUsersToChangePassword: true
            PasswordReusePrevention: 2
            MaxPasswordAge: 90
            HardExpiry: false
```

**Note:** Password policy is just an account setup, there can be no more than one password policy on the account.
Deploying another password policy on same account will override existing one.

# Properties

## `MinimumPasswordLength` - int

Minimum number of characters allowed in an IAM user password.

## `RequireLowercaseCharacters` - boolean

Specifies whether IAM user passwords must contain at least one lowercase character from the ISO basic Latin alphabet (a
to z).

## `RequireUppercaseCharacters` - boolean

Specifies whether IAM user passwords must contain at least one uppercase character from the ISO basic Latin alphabet (A
to Z).

## `RequireNumbers` - boolean

Specifies whether IAM user passwords must contain at least one numeric character (0 to 9).

## `RequireSymbols` - boolean

Specifies whether IAM user passwords must contain at least one of the following non-alphanumeric characters.

## `AllowUsersToChangePassword` - boolean

Allows all IAM users in your account to use the AWS Management Console to change their own passwords.

## `PasswordReusePrevention` - int

Specifies the number of previous passwords that IAM users are prevented from reusing.

## `MaxPasswordAge` - int

The number of days that an IAM user password is valid.

## `HardExpiry` - boolean

Prevents IAM users from setting a new password after their password has expired.

# Output values

## `PhysicalId` - string (physical resource ID)

Fixed string to identify resource.
