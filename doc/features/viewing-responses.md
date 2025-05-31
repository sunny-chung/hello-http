---
title: Viewing Responses
---

# Viewing Responses

## Connection Security

![Connection Security](../connection-security.gif)

From v1.4.3 onwards, the connection security level and certificates used would be displayed in the top-right corner of
the response viewer.

The possible indicator icons are described below.

| Icon                                                   | Name           | Description                                                                                                                                                                  |
|--------------------------------------------------------|----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ![Not encrypted](../conn-security-unencrypted.png)     | Not encrypted  | The connection is not encrypted. Anybody with access to the wire is able to intercept and manipulate the traffic.                                                            |
| ![Unverified TLS](../conn-security-unverified-tls.png) | Unverified TLS | The connection is encrypted, but the certificates are not verified. It is vulnerable to [man-in-the-middle attacks](https://en.wikipedia.org/wiki/Man-in-the-middle_attack). |
| ![One-way TLS](../conn-security-tls.png)               | One-way TLS    | The connection is encrypted, and the server certificates are trusted according to the environment SSL setting.                                                               |
| ![mTLS](../conn-security-mtls.png)                     | mTLS           | The connection is encrypted, and the client certificate and the server certificates are mutually trusted.                                                                    |

More details of the established TLS connection are available in the [Transport Timeline](transport-timeline).

## Searching

Click the response, then press Ctrl-F or Command-F to bring up a search bar.

![Searching in Response](../response-searching.png)

## Filtering JSON by JSON Path

If a response is recognized as JSON (according to the `Content-Type` header), a text box would appear at the bottom to
allow entry of a JSON path to filter the response. The supported expressions are
documented [here](https://github.com/json-path/JsonPath).

![Filtering JSON Response](../response-json-filtering.gif)

## Collapsing/Expanding JSON

JSON arrays and objects can be collapsed and expanded.

![Collapse/Expand JSON Data Structures](../collapse-json.gif)

## Copying Requests and Responses

![Copying Responses](../copy-response.gif)

### Copy Individual Fields

Hover any copiable fields and click the "copy" button to copy. A prompt is displayed if copying is successful.

### Copy Entire Request and Response

This is designed for communication with third party about API integration issues. When a third party API does not work as expected, simply copy the whole request and response to them, and they will have commonly needed information to figure out and fix the issues.

To use it, just click the "Copy All" button. This functionality is supported regardless of protocols and streaming types.

The copied text is in a markdown-like format for readability.

Sample of copied text of a simple HTTP call:
``````markdown
Test POST Request - with Unicode JSON body
==========================================

Request
-------
Start Time: 2024-02-18 23:33:31.426 (+08:00)

HTTP/2.0

POST https://httpbin.org/anything/%E4%B8%AD%E6%96%87?a=%E4%B8%AD%E6%96%87+a

Headers:
`````
:method: POST
:scheme: https
:authority: httpbin.org
:path: /anything/%E4%B8%AD%E6%96%87?a=%E4%B8%AD%E6%96%87+a
abc??: ????
user-agent: Hello-HTTP/1.5.0-SNAPSHOT
content-type: application/json; charset=UTF-8
`````

Body:
`````
{
  "abc": "中文字"  
}

`````

Response
--------
Completion Time: 2024-02-18 23:33:32.545 (+08:00)

Duration: 1.119s

Status Code: 200 OK

Headers:
`````
date: Sun, 18 Feb 2024 15:33:32 GMT
content-type: application/json
content-length: 592
server: gunicorn/19.9.0
access-control-allow-origin: *
access-control-allow-credentials: true
`````

Body:
`````
{
  "args": {
    "a": "\u4e2d\u6587 a"
  }, 
  "data": "{\n  \"abc\": \"\u4e2d\u6587\u5b57\"  \n}", 
  "files": {}, 
  "form": {}, 
  "headers": {
    "Abc-\u0087": "-\u0087WC", 
    "Content-Type": "application/json; charset=UTF-8", 
    "Host": "httpbin.org", 
    "Transfer-Encoding": "chunked", 
    "User-Agent": "Hello-HTTP/1.5.0-SNAPSHOT", 
    "X-Amzn-Trace-Id": "Root=1-65d2234c-12eebfb53b72ab5f3da07d49"
  }, 
  "json": {
    "abc": "\u4e2d\u6587\u5b57"
  }, 
  "method": "POST", 
  "origin": "42.2.62.108", 
  "url": "https://httpbin.org/anything/\u4e2d\u6587?a=\u4e2d\u6587+a"
}

`````

``````

Sample of copied text of a GraphQL subscription call:
``````markdown
Test GraphQL subscription - Intervals
=====================================

Request
-------
Start Time: 2024-02-18 23:35:03.259 (+08:00)

HTTP/1.1

GET wss://localhost:8080/graphql

Headers:
`````
Connection: Upgrade
Host: localhost:8080
Sec-WebSocket-Key: 2z8mVQPxl4GjWJ9JBOfv1g==
Sec-WebSocket-Version: 13
Upgrade: websocket
User-Agent: Hello-HTTP/1.5.0-SNAPSHOT
`````

Response
--------
Status Code: 101 Switching Protocols

Headers:
`````
connection: upgrade
sec-websocket-accept: 3Wr0cZspTc93P9b5MvWs3ggeN5s=
upgrade: websocket
`````

Incoming #1
-----------
Time: 2024-02-18 23:35:03.428 (+08:00)

Body:
`````
{"data":{"interval":{"id":0,"instant":"2024-02-18T23:35:03.426589+08:00[Asia/Hong_Kong]"}}}
`````


Incoming #2
-----------
Time: 2024-02-18 23:35:04.431 (+08:00)

Body:
`````
{"data":{"interval":{"id":1,"instant":"2024-02-18T23:35:04.429381+08:00[Asia/Hong_Kong]"}}}
`````


Incoming #3
-----------
Time: 2024-02-18 23:35:05.437 (+08:00)

Body:
`````
{"data":{"interval":{"id":2,"instant":"2024-02-18T23:35:05.435635+08:00[Asia/Hong_Kong]"}}}
`````


Incoming #4
-----------
Time: 2024-02-18 23:35:06.439 (+08:00)

Body:
`````
{"data":{"interval":{"id":3,"instant":"2024-02-18T23:35:06.438091+08:00[Asia/Hong_Kong]"}}}
`````


End
---
Completion Time: 2024-02-18 23:35:06.441 (+08:00)

Duration: 3.182s

Closed by us with code 1000

``````

Sample of copied text of a gRPC bidirectional call:
``````markdown
My gRPC Request - Bidirectional
===============================

Request
-------
Start Time: 2024-02-18 23:49:43.670 (+08:00)

HTTP/2.0

POST grpcs://localhost:9091

Headers:
`````
:authority: localhost:9091
:path: /sunnychung.grpc.services.MyService/BiStream
:method: POST
:scheme: https
content-type: application/grpc
te: trailers
user-agent: grpc-java-netty/1.59.1
grpc-accept-encoding: gzip
`````

Outgoing #1
-----------
Time: 2024-02-18 23:49:44.882 (+08:00)

Body:
`````
{
  "data": 23  
}
`````


Incoming #1
-----------
Time: 2024-02-18 23:49:44.884 (+08:00)

Body:
`````
{
  "data": 123
}
`````


Outgoing #2
-----------
Time: 2024-02-18 23:49:46.415 (+08:00)

Body:
`````
{
  "data": 456  
}
`````


Incoming #2
-----------
Time: 2024-02-18 23:49:46.417 (+08:00)

Body:
`````
{
  "data": 556
}
`````


End
---
Completion Time: 2024-02-18 23:49:47.105 (+08:00)

Duration: 3.435s

Headers:
`````
:status: 200
content-type: application/grpc
grpc-encoding: identity
grpc-accept-encoding: gzip
grpc-status: 0
`````

``````

### Copy Entire Transport Log

Click "Copy" button in the "Raw" tab.

Copied texts are in a table format. It can be imported into spreadsheet softwares, or selected or manipulated in major code editors that support vertical selections, for example, Sublime Text. Don't forget to turn off "word wrap" while viewing the table in code editors.

Sample of copied text:
``````
Test POST Request - with Unicode JSON body
==========================================

Time                             | Dir | Stream | Detail                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
=================================|=====|========|===============================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================
2024-02-18 23:33:31.432 (+08:00) | -   | *      | DNS resolution of domain [httpbin.org] started
2024-02-18 23:33:31.512 (+08:00) | -   | *      | DNS resolved to [httpbin.org/35.171.123.176, httpbin.org/3.230.23.0]
2024-02-18 23:33:31.513 (+08:00) | -   | *      | Connecting to httpbin.org/35.171.123.176:443
2024-02-18 23:33:31.758 (+08:00) | -   | *      | Connected to httpbin.org/35.171.123.176:443 with HTTP/2.0
2024-02-18 23:33:32.290 (+08:00) | -   | *      | Established TLS upgrade with protocol 'TLSv1.2', cipher suite 'TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256' and application protocol 'h2'.
                                 |     |        | 
                                 |     |        | Client principal = null
                                 |     |        | 
                                 |     |        | Server principal = CN=httpbin.org
                                 |     |        | 
2024-02-18 23:33:32.300 (+08:00) | Out | *      | Frame: SETTINGS (0x4); flags: (0x0); length: 36
                                 |     |        | HEADER_TABLE_SIZE: 8192
                                 |     |        | ENABLE_PUSH: 1
                                 |     |        | MAX_CONCURRENT_STREAMS: 250
                                 |     |        | INITIAL_WINDOW_SIZE: 65535
                                 |     |        | MAX_FRAME_SIZE: 65536
                                 |     |        | MAX_HEADER_LIST_SIZE: 16777215
                                 |     |        | 
                                 |     |        | 
2024-02-18 23:33:32.301 (+08:00) | In  | *      | Frame: SETTINGS (0x4); flags: (0x0); length: 18
                                 |     |        | MAX_CONCURRENT_STREAMS: 128
                                 |     |        | INITIAL_WINDOW_SIZE: 65536
                                 |     |        | MAX_FRAME_SIZE: 16777215
                                 |     |        | 
                                 |     |        | 
2024-02-18 23:33:32.302 (+08:00) | In  | *      | Frame: WINDOW_UPDATE (0x8); flags: (0x0); length: 4
                                 |     |        | Increment 2147418112
                                 |     |        | 
                                 |     |        | 
2024-02-18 23:33:32.308 (+08:00) | Out | *      | Frame: WINDOW_UPDATE (0x8); flags: (0x0); length: 4
                                 |     |        | Increment 2147418112
                                 |     |        | 
                                 |     |        | 
2024-02-18 23:33:32.308 (+08:00) | Out | *      | Frame: SETTINGS (0x4); flags: ACK (0x1); length: 0
                                 |     |        | 
2024-02-18 23:33:32.309 (+08:00) | Out | 1      | Frame: HEADERS (0x1); flags: END_HEADERS (0x4); length: 116
                                 |     |        | [INDEXED        (3)] :method: POST
                                 |     |        | [INDEXED        (7)] :scheme: https
                                 |     |        | [NAME INDEXED   (1)] :authority: httpbin.org
                                 |     |        | [NAME INDEXED   (4)] :path: /anything/%E4%B8%AD%E6%96%87?a=%E4%B8%AD%E6%96%87+a
                                 |     |        | [+ NEW INDEX       ] abc中文: 中文字元
                                 |     |        | [NAME INDEXED  (58)] user-agent: Hello-HTTP/1.5.0-SNAPSHOT
                                 |     |        | [NAME INDEXED  (31)] content-type: application/json; charset=UTF-8
                                 |     |        | 
                                 |     |        | 
2024-02-18 23:33:32.309 (+08:00) | Out | 1      | Frame: DATA (0x0); flags: (0x0); length: 26
                                 |     |        | {
                                 |     |        |   "abc": "中文字"  
                                 |     |        | }
                                 |     |        | 
2024-02-18 23:33:32.310 (+08:00) | Out | 1      | Frame: DATA (0x0); flags: END_STREAM (0x1); length: 0
                                 |     |        | 
2024-02-18 23:33:32.532 (+08:00) | In  | *      | Frame: SETTINGS (0x4); flags: ACK (0x1); length: 0
                                 |     |        | 
2024-02-18 23:33:32.534 (+08:00) | In  | 1      | Frame: WINDOW_UPDATE (0x8); flags: (0x0); length: 4
                                 |     |        | Increment 26
                                 |     |        | 
                                 |     |        | 
2024-02-18 23:33:32.541 (+08:00) | In  | 1      | Frame: HEADERS (0x1); flags: END_HEADERS (0x4); length: 114
                                 |     |        | Update dynamic table size to 0
                                 |     |        | [INDEXED        (8)] :status: 200
                                 |     |        | [NAME INDEXED  (33)] date: Sun, 18 Feb 2024 15:33:32 GMT
                                 |     |        | [NAME INDEXED  (31)] content-type: application/json
                                 |     |        | [NAME INDEXED  (28)] content-length: 592
                                 |     |        | [- NOT INDEXED     ] server: gunicorn/19.9.0
                                 |     |        | [- NOT INDEXED     ] access-control-allow-origin: *
                                 |     |        | [- NOT INDEXED     ] access-control-allow-credentials: true
                                 |     |        | 
                                 |     |        | 
2024-02-18 23:33:32.544 (+08:00) | In  | 1      | Frame: DATA (0x0); flags: (0x0); length: 592
                                 |     |        | {
                                 |     |        |   "args": {
                                 |     |        |     "a": "\u4e2d\u6587 a"
                                 |     |        |   }, 
                                 |     |        |   "data": "{\n  \"abc\": \"\u4e2d\u6587\u5b57\"  \n}", 
                                 |     |        |   "files": {}, 
                                 |     |        |   "form": {}, 
                                 |     |        |   "headers": {
                                 |     |        |     "Abc-\u0087": "-\u0087WC", 
                                 |     |        |     "Content-Type": "application/json; charset=UTF-8", 
                                 |     |        |     "Host": "httpbin.org", 
                                 |     |        |     "Transfer-Encoding": "chunked", 
                                 |     |        |     "User-Agent": "Hello-HTTP/1.5.0-SNAPSHOT", 
                                 |     |        |     "X-Amzn-Trace-Id": "Root=1-65d2234c-12eebfb53b72ab5f3da07d49"
                                 |     |        |   }, 
                                 |     |        |   "json": {
                                 |     |        |     "abc": "\u4e2d\u6587\u5b57"
                                 |     |        |   }, 
                                 |     |        |   "method": "POST", 
                                 |     |        |   "origin": "42.2.62.108", 
                                 |     |        |   "url": "https://httpbin.org/anything/\u4e2d\u6587?a=\u4e2d\u6587+a"
                                 |     |        | }
                                 |     |        | 
                                 |     |        | 
2024-02-18 23:33:32.544 (+08:00) | In  | 1      | Frame: DATA (0x0); flags: END_STREAM (0x1); length: 0
                                 |     |        | 
2024-02-18 23:33:32.546 (+08:00) | Out | *      | Frame: GOAWAY (0x7); flags: (0x0); length: 25
                                 |     |        | Last stream 0
                                 |     |        | Code NO_ERROR
                                 |     |        | Graceful shutdown
                                 |     |        | 
                                 |     |        | 
2024-02-18 23:33:32.774 (+08:00) | -   | *      | Response completed

``````

