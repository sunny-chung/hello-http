---
title: HTTP/2
---

# HTTP/2

![Setting](../http-protocol-version.png)

By default, Hello HTTP tries to upgrade to HTTP/2 whenever possible for SSL protocols, and uses HTTP/1.1 otherwise. This behaviour can be changed in
the [Environment setting](../features/environments). This setting has no effect to WebSocket, GraphQL subscriptions and
gRPC connections, which require a specific HTTP version to run.

## Transport Timeline

HTTP/2 is a binary protocol over a single HTTP connection. Hello HTTP logs decoded frames to the Transport Timeline to
make it informational. Besides decoded frame headers and bodies, stream IDs and header IDs are also logged.

The numbers enclosed in curly brackets are stream IDs. For example, `{1}` represents stream 1. `{*}` is not
stream-specific and applies to the whole connection.

![Example](../timeline-http2.png)

Note that some non-informational frame headers are not logged, for example, padding.

## Push Promises
~~There is no concrete support to push promises as they are deprecated. Nonetheless, their responses can be found in the
Transport Timeline.~~

Push promise support has been removed since Hello HTTP v1.6.0.
