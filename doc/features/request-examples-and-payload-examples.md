---
title: Request Examples & Payload Examples
---

# Request Examples & Payload Examples

This differentiates Hello HTTP with other HTTP client softwares. Each Request contains one "Base Example" and zero or more Request Examples.
Each example *can* contain different request body, query parameters, headers and pre-flight actions. They may inherit
from the "Base Example" to share identical properties. All these examples of a Request share the same application
protocol, HTTP method and path. 

This allows testing different use cases and error cases against the same API. 

## Overriding Base Examples

### Request Body

By default, body with most of the content types (except "Multipart Form" and "Form URL-Encoded") is not inherited. The
value in individual Request Example overrides the Base Example.

Body with content type "None" or "Binary File" cannot be inherited.

![Inheriting Request Body](../inherit-request-body.png)

Body with remaining content types can be changed to inherit from the Base Example, by unselecting the
"Is Override Base?" checkbox.

### Other Stuffs

By default, following items are inherited from the "Base Example" of the same Request.
- Body with content type "Multipart Form" or "Form URL-Encoded"
- Query parameters
- Headers
- Post flight actions - "update environment variable by response headers"
- Post flight actions - "update environment variable by response bodies"

They will be displayed under the "Inherited" section.

![Overriding](../example-override1.png)

Among these items, individual inheritance can be disabled by unselecting the checkbox.

## Renaming a Request Example
Just double-click the Request Example, make changes and hit enter.

## Duplicating a Request Example
Click the duplicate button next to the name of the Example, and then give a name to the new one.

![Dupplicating a Request Example](../duplicate-request-example.gif)

## Navigating among Request Examples
Request Examples can be navigated by scrolling the Example tabs in horizontal direction, vertical direction or clicking the "List" button next to the "Add Request Example" button.

![Navigating among Request Examples](../navigate-request-examples.gif)

## Payload Examples

For WebSocket and gRPC calls that support client streaming, each request contains one or more Payload Example to allow
multiple message templates. Different from Request Examples, Payload Examples have no inheritance.

![Payload Examples](../payload-examples.png)
