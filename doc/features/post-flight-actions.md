---
title: Post Flight Actions
---

# Post Flight Actions

![Post Flight](../screenshot2.png)

As the name implies, it allows defining actions to be executed after a response is received.

If it is supported on a particular Request type, the "Post Flight" tab will appear.

Inheritance is supported, in a way similar to that documented in [Request Examples & Payload Examples](request-examples-and-payload-examples).

## Update environment variables according to response headers

Update environment variables using values of particular response headers.

For example, input "resourceId" as "Variable" and "ETag" as "Header", when a response is received, the value of the
header "ETag" will be set to the environment variable "resourceId".

To make it works, an environment must be selected before firing Requests.

## Update environment variables according to response bodies

Update environment variables using response body. JSON paths need to be specified.

If "$" is inputted, the content of the whole response body will be set to an environment variable, regardless of
response content types.

Otherwise, a valid JSON path is expected to be inputted, for example, `$.accessToken`, which instructs to take the
value of the field `accessToken` from the response JSON and set it to an environment variable.

To make it works, an environment must be selected before firing Requests, the Response content type must be JSON, and
the JSON path inputted must be valid.


