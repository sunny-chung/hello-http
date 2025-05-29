---
title: Cookie
---

# HTTP Cookie

## Enable or Disable

To enable Cookie support, first enable in the Subproject Configuration dialog. It would affect the whole Subproject.

By default, Cookie is disabled.

![Enable Cookie Step 1](../cookie-enable-step-1.png)

![Enable Cookie Step 2](../cookie-enable-step-2.png)

## Receive Cookie

To receive Cookie from servers, an environment must be selected or created. Cookie storage is not shared among multiple environments.

## Send Cookie

If Cookie is enabled, the Cookie storing in the storage would be applied to matching requests automatically. To examine what Cookie would be applied, check the Cookie tab in the Request Example level.

To send custom Cookie in additional to stored Cookie, one can insert Cookie in the Cookie tab in the Request Example level:

![Cookie Editor in Request Example](../cookie-editor-in-request-example.png)

or in the Environment level:

![Cookie Editor 1 in Environment](../cookie-editor-1-in-environment.png)

![Cookie Editor 2 in Environment](../cookie-editor-2-in-environment.png)

## Variables and Functions

Variables and functions are supported only in the "value" field.

However, Cookie containing variable and function macros in the "Set-Cookie" response headers are automatically dropped for security reasons.

For example, in the following response, only the cookie "cookie1" would be stored.

```
HTTP/1.1 204 No Content
Set-Cookie: cookie0=some-${{var}}; Max-Age=3600
Set-Cookie: cookie1=AbcDE; Max-Age=3600
```

## Applicable Transports

All types of supported HTTP transport, including WebSocket, GraphQL and gRPC, support Cookie. Note that response headers "Set-Cookie" only take effect after the entire request is completed.
