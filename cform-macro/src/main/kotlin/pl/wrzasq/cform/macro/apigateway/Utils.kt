/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.apigateway

/**
 * Builds API Gateway URI for Lambda invocation.
 *
 * @param lambda Lambda reference.
 * @return Integration URI.
 */
fun lambdaUri(lambda: String) = "arn:\${AWS::Partition}:apigateway:\${AWS::Region}:lambda:path/2015-03-31/functions/" +
    "arn:\${AWS::Partition}:lambda:\${AWS::Region}:\${AWS::AccountId}:function:$lambda/invocations"
