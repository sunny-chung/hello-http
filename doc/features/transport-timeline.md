---
title: Transport Timeline
---

# Transport Timeline

Connection events and raw transport data can be viewed real-time in Transport Timeline. This is for low-level diagnosis
purpose.

Depends on the application protocol, the displayed information may be different. In general, most protocols include:
- Before DNS resolution
- DNS resolution result
- Before connecting to a remote host
- Result of connecting to a remote host
- Result of a TLS handshake (if SSL is applicable)
- Connection upgrade (if applicable)

Currently, entire request and response bodies are logged in this timeline, including binary data. This might be reduced
in the future.

Below shows some examples.

## HTTP/1.1
![Example](../timeline-http1.png)

## HTTP/2
![Example](../timeline-http2.png)
